# SQLite-like-driver

Данный групповой проект посвящен разработке собственной базы данных, похожей на SQLite. Продукт предназначен для хранения, извлечения и изменения данных в виде таблиц. Итоговая цель проекта - реализация собственного JDBC-драйвера, работающего с колоночной архитектурой SQLite.

## Авторы
- Сидоренко Софья, гр. 24215
- Мункуев Владислав, гр. 24215
- Образцов Дмитрий, гр. 24214
- Романенко Никита, гр. 24214

## Требования 

- Java 17 или выше
- Gradle Wrapper (входит в репозиторий, отдельная установка Gradle не требуется)

## Сборка проекта

На данном этапе для сборки проекта необходимо проделать следующие шаги: 
- Сгенерируйте файлы грамматики ANTLR (Linux / macOS):

```bash
./gradlew generateGrammarSource
```
Или для Windows:

```bash
gradlew.bat generateGrammarSource
```
- Соберите проект:

```bash
./gradlew clean build
```
Или для Windows:

```bash
gradlew.bat clean build
```

## Запуск проекта

После сборки проект запускается как консольное приложение и принимает SQL-запросы из стандартного ввода.

Запуск через Gradle (Linux / macOS):

```bash
./gradlew run
```

Для Windows:

```bash
gradlew.bat run
```

## Функционал 

После запуска приложения можно вводить SQL-команды.

- Работа с базами данных:
    - `CREATE DATABASE db_name` - создание базы данных
    - `DROP DATABASE db_name` - удаление базы данных
    - `USE db_name` - подключение к базе данных

- Работа с таблицами: 
    - `CREATE TABLE table_name ( column1 DataType [CONSTRAINTS], column2 DataType [CONSTRAINTS], ...)` - создание таблицы
    - `DROP TABLE table_name` - удаление таблицы
    - `ALTER TABLE table_name RENAME TO new_table_name` - переименование таблицы

- Работа со столбцами:
    - `ALTER TABLE table_name ADD COLUMN column_definition` - добавление столбца 
    - `ALTER TABLE table_name DROP COLUMN column_name` - удаление столбца 
    - `ALTER TABLE table_name RENAME COLUMN old_name TO new_name` - переименование столбца

- Вставка данных: 
    - `INSERT INTO table_name VALUES (…)` - вставка строки в таблицу

- Выбор данных:
    - `SELECT * FROM table_name` - выбор всех данных из таблицы (всех колонок)
    - `SELECT column1, column2 FROM table_name` - выбор столбцов 
    - `SELECT column1, column2 FROM table_name WHERE column = value` - выбор с условием

## Пример работы

После запуска приложения можно вводить SQL-команды.

Создание и выбор базы данных:
```bash
CREATE DATABASE testDB;
USE testDB;
```

Создание таблицы:
```bash
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    age INTEGER
);
```

Добавление столбца:
```bash
ALTER TABLE users ADD COLUMN email TEXT;
```

Удаление столбца:
```bash
ALTER TABLE users DROP COLUMN email;
```

Удаление таблицы:
```bash
DROP TABLE users;
```

Удаление базы данных:
```bash
DROP DATABASE testDB;
```

## Структура проекта

Simple-DB-Driver/
│
├─ .gradle/                  # Внутренние файлы Gradle
├─ .idea/                    # Настройки IDE (IntelliJ IDEA)
├─ build/                    # Сборка проекта
├─ gradle/                   # Скрипты Gradle
├─ gradlew, gradlew.bat      # Wrapper для Gradle
├─ settings.gradle           # Конфигурация Gradle
├─ build.gradle              # Сборка проекта
│
├─ src/main/java/            # Исходный код проекта
│   ├─ Exceptions/           # Пользовательские исключения
│   │   ├─ AlreadyExistsException.java
│   │   ├─ EmptyFileException.java
│   │   ├─ FileManagerException.java
│   │   ├─ FileStorageException.java
│   │   ├─ FileTypeException.java
│   │   ├─ NoDataBaseException.java
│   │   ├─ NoFileException.java
│   │   ├─ PermissionDeniedException.java
│   │   └─ SerializationStorageException.java
│   │
│   ├─ FileWork/             # Работа с файловым хранилищем
│   │   ├─ JSON/             # Реализация JSON-хранилища
│   │   │   ├─ JacksonConfig.java
│   │   │   └─ JsonFileStorage.java
│   │   ├─ Metadata/         # Метаданные базы, таблиц и колонок
│   │   ├─ FileManager.java  # Основной класс работы с БД и таблицами
│   │   ├─ FileStorage.java  # Интерфейс работы с файловым хранилищем
│   │   └─ PathManager.java  # Генерация путей к файлам БД и таблиц
│   │
│   ├─ SqlParser/            # SQL-парсер и структура запросов
│   │   ├─ Antlr/            # Генерированные ANTLR-файлы и парсер
│   │   │   └─ файлы грамматики и интерпретаторы
│   │   ├─ QueriesStruct/    # Классы запросов и интерфейсы
│   │   │   ├─ Queries.java
│   │   │   └─ QueryInterface.java
│   │
│   ├─ DataStruct/           # Структуры данны
│   └─ Main.java             # Точка входа, консольный интерфейс
│
├─ src/test/java/            # Тесты проекта
│   ├─ ...
│
├─ src/main/antlr/SQL.g4     # Файл грамматики SQL для ANTLR
├─ data/                     # Данные для примера работы приложения
│   ├─ ...
└─ gen/                      # Сгенерированные ANTLR-классы

## Исключения 

Для некоторых определённых ошибок написаны собственные классы исключений. На данном этапе обрабатываются исключения: 

- Файл или база данных не найдены (NoFileException)
- Ошибки доступа к файлам (PermissionDeniedException)
- Пустые файлы (EmptyFileException)
- Конфликты имен (AlreadyExistsException)
- Ошибки сериализации и хранения (SerializationStorageException, FileStorageException)

По мере дальнейшей разработки проекта исключения будут расширяться.

## Тесты

Для проверки работы приложения написаны тесты.
На данный момент покрыты основные функции работы с файловым хранилищем.
По мере дальнейшей разработки проекта тесты будут расширяться. 