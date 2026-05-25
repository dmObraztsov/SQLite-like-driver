# SQLite-like Driver

Групповой проект - собственная реляционная база данных с колоночным бинарным хранилищем.
## Авторы

- Сидоренко Софья, гр. 24215
- Мункуев Владислав, гр. 24215
- Образцов Дмитрий, гр. 24214
- Романенко Никита, гр. 24214

## Требования

- Java 17 или выше
- Gradle Wrapper (входит в репозиторий, отдельная установка Gradle не требуется)

## Сборка и запуск проекта

Перед сборкой проекта необходимо загрузить файлы репозитория:

https://github.com/AnarCom/db-test


```bash
./gradlew shadowJar
```

Собранный fat-jar появится в `build/libs/Simple-DB-Driver-1.0-SNAPSHOT.jar`.

```bash
java -jar "your_path\build\libs\Simple-DB-Driver-1.0-SNAPSHOT.jar" --init "your_path_2\db-test\schema.sql"
```

В данной строке:
- `your_path` - ваш путь до папки build проекта
- `your_path_2` - ваш путь до загруженного репозитория db-test


## Запуск тестов

Запуск тестов проекта:

```bash
./gradlew test
```

Запуск тестов, выбранных в качестве критерия успеха:

**Перед запуском тестов необходимо собрать проект как в разделе "Сборка и запуск проекта"**. Затем: 

```bash
pip install psycopg2-binary
```

```bash
cd "your-path\db-test" && python run_tests.py
```

Здесь: 
`your_path` - ваш путь до загруженного репозитория db-test


## Функционал

### DDL - работа с базами данных

```sql
CREATE DATABASE db_name
DROP DATABASE db_name
USE db_name
```

### DDL - работа с таблицами

```sql
CREATE TABLE table_name (
    column1 DataType [CONSTRAINTS],
    column2 DataType [CONSTRAINTS],
    ...
)

DROP TABLE [IF EXISTS] table_name
ALTER TABLE table_name RENAME TO new_name
ALTER TABLE table_name ADD COLUMN column_definition
ALTER TABLE table_name DROP COLUMN column_name
ALTER TABLE table_name RENAME COLUMN old_name TO new_name
```

### DML - изменение данных

```sql
INSERT INTO table_name (col1, col2, ...) VALUES (val1, val2, ...)
INSERT INTO table_name VALUES (val1, val2, ...)

UPDATE table_name SET col = value [WHERE condition]

DELETE FROM table_name [WHERE condition]
```

### SELECT - выборка данных

```sql

SELECT * FROM table_name
SELECT col1, col2 FROM table_name [WHERE condition]

SELECT ... ORDER BY col [ASC|DESC] [LIMIT n [OFFSET m]]

SELECT DISTINCT col FROM table_name

SELECT COUNT(*), SUM(col), AVG(col), MIN(col), MAX(col) FROM table_name

SELECT col, COUNT(*) FROM table_name
GROUP BY col
[HAVING condition]

SELECT ... FROM t1 [LEFT | INNER] JOIN t2 ON t1.col = t2.col
```

### WHERE — условия

```sql
WHERE col = value
WHERE col > value AND col2 < value
WHERE col IN (val1, val2, ...)
WHERE col BETWEEN val1 AND val2
WHERE col LIKE 'pattern%'
WHERE col IS [NOT] NULL
```

### Constraints

| Ограничение | Описание |
|---|---|
| `PRIMARY KEY` | Уникальный ненулевой идентификатор строки |
| `NOT NULL` | Запрет NULL-значений |
| `UNIQUE` | Уникальность значений в колонке |
| `DEFAULT value` | Значение по умолчанию |
| `AUTOINCREMENT` | Автоматически увеличиваемое целое число |
| `CHECK (expr)` | Проверка условия при вставке/обновлении |

### TCL — транзакции

```sql
BEGIN TRANSACTION
COMMIT
ROLLBACK
```

Транзакции реализованы через WAL (Write-Ahead Logging): до применения изменений исходное состояние колонок сохраняется на диск, что обеспечивает корректное восстановление после сбоев.

## Формат хранения данных

Каждая колонка таблицы хранится в отдельном бинарном файле. Формат файла:

- **Заголовок**: magic bytes `DBCL` + количество строк (4 байта)
- **Таблица смещений**: для каждой строки — пара `(offset, length)`
- **Секция данных**: значения в кодировке UTF-8

Такой формат позволяет при SELECT читать только файлы нужных колонок, не загружая лишние данные.

## Индексация

Хеш-индексация на базе `HashMap<String, List<Integer>>`: каждому значению колонки соответствует список номеров строк. Индекс ускоряет поиск по точному совпадению и автоматически обновляется при изменении данных.

## Структура проекта

```
src/main/java/
├── Exceptions/                  # Пользовательские исключения
├── FileWork/
│   ├── Binary/
│   │   └── BinaryFileStorage.java   # Бинарное колоночное хранилище
│   ├── Index/
│   │   └── ColumnIndex.java         # Хеш-индекс по колонке
│   ├── JSON/                        # JSON-хранилище (альтернативная реализация)
│   ├── Metadata/                    # Метаданные БД, таблиц и колонок
│   ├── WAL/
│   │   └── WalEntry.java            # Запись журнала транзакций
│   ├── FileManager.java             # Основной слой работы с БД
│   ├── FileStorage.java             # Интерфейс хранилища
│   └── PathManager.java             # Генерация путей к файлам
├── SqlParser/
│   ├── Antlr/
│   │   ├── AntlrParser.java         # Обход дерева разбора, построение запросов
│   │   └── SQLProcessor.java        # Фасад: строка SQL → QueryInterface
│   └── QueriesStruct/
│       ├── Queries.java             # Все типы запросов
│       ├── QueryInterface.java      # Интерфейс исполнения запроса
│       ├── WhereCondition.java      # Условия WHERE/HAVING
│       └── ExecutionResult.java     # Результат выполнения запроса
├── Yadro/DataStruct/
│   ├── DatabaseEngine.java          # Движок исполнения запросов
│   ├── Column.java                  # Колонка с данными
│   ├── Row.java                     # Строка результата
│   ├── ColumnMetadata.java          # Описание колонки (тип, constraints)
│   ├── DataType.java                # Типы данных
│   ├── Constraints.java             # Виды ограничений
│   └── Collate.java                 # Правила сравнения
├── com/simpledb/
│   ├── jdbc/                        # JDBC-драйвер
│   │   ├── SimpleDriver.java
│   │   ├── SimpleConnection.java
│   │   ├── SimpleStatement.java
│   │   ├── SimplePreparedStatement.java
│   │   ├── SimpleResultSet.java
│   │   ├── SimpleResultSetMetaData.java
│   │   └── SimpleDatabaseMetaData.java
│   └── server/
│       ├── PostgresServer.java      # TCP-сервер (PostgreSQL wire protocol)
│       └── ClientHandler.java       # Обработка подключений
└── Main.java                        # Точка входа
src/main/antlr/
└── SQL.g4                           # Грамматика SQL для ANTLR4
src/test/java/                       # Тесты (138 тестов, 3 уровня)
```

## Исключения

| Класс | Ситуация |
|---|---|
| `NoFileException` | Файл или база данных не найдены |
| `NoDataBaseException` | Обращение к несуществующей базе |
| `NoTableException` | Обращение к несуществующей таблице |
| `PermissionDeniedException` | Ошибка доступа к файлам |
| `EmptyFileException` | Обнаружен пустой файл данных |
| `AlreadyExistsException` | Конфликт имён при создании объекта |
| `FileStorageException` | Ошибка чтения/записи хранилища |
| `SerializationStorageException` | Ошибка сериализации данных |
| `FileManagerException` | Ошибка на уровне файлового менеджера |
| `FileTypeException` | Несовместимый тип файла |

## Тестирование

Реализовано три уровня тестирования:

- **Модульные тесты** — компоненты в изоляции: `BinaryFileStorageTest`, `ColumnIndexTest`, `FileManagerTest`, `AntlrParserTest`, `DatabaseEngineTest` и другие
- **Интеграционные тесты** — взаимодействие всех слоёв (`IntegrationTest`)
- **E2E-тесты** — SQL-запросы выполняются против реального сервера, результаты сравниваются с эталонными ответами PostgreSQL 17
