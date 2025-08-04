# FlameCore

FlameCore is a comprehensive and lightweight library designed to streamline Spigot and BungeeCord plugin development. It provides a suite of intuitive APIs for creating modern, feature-rich Minecraft plugins with minimal boilerplate code. FlameCore supports a wide range of functionalities, including rich text formatting, command management, GUI menus, database integration, in-game notifications, material handling, and schematic operations, all while ensuring compatibility across different Minecraft server versions. FlameCore is not a standalone plugin but is intended to be packaged within your plugin.

## Features

- **ColorAPI**: Format chat messages with legacy codes, hex colors, gradients, and interactive click/hover events.
- **CommandAPI**: Build complex, type-safe commands with subcommands and permissions using a fluent builder.
- **MenuAPI**: Create protected, animated GUI menus with customizable click actions and item behaviors.
- **SQLiteAPI & MySQLAPI**: Manage data with simple, annotation-based persistence for local SQLite or remote MySQL databases.
- **TitleAPI**: Send formatted titles and subtitles to players with customizable timings.
- **ActionBarAPI**: Display single-line messages above players' hotbars with advanced formatting.
- **BossBarAPI**: Create dynamic boss bars with customizable text, progress, colors, and styles.
- **MaterialAPI**: Safely handle materials across Minecraft versions with fallback support and helper methods.
- **SchematicAPI**: Save, load, and paste block structures for arenas or restorable blocks.
- **BlocksAPI**: Capture and restore individual block states with asynchronous, lag-free processing.
- **ConfigAPI**: Manage configuration files with a clean, centralized interface for reading and writing values.
- **LangAPI**: Handle localized messages with placeholder support and integration with PlaceholderAPI.

## Installation

To use FlameCore in your Maven project, add the JitPack repository and the FlameCore dependency to your `pom.xml`. Since FlameCore is packaged within your plugin, you must shade and relocate its classes to avoid conflicts.

### Step 1: Add the JitPack Repository

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Step 2: Add the FlameCore Dependency

```xml
<dependency>
    <groupId>com.github.ArkFlame</groupId>
    <artifactId>FlameCore</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Step 3: Shade and Relocate FlameCore

Use the Maven Shade Plugin to bundle FlameCore into your plugin's JAR and relocate its packages to your plugin's package namespace (e.g., `${project.groupId}.flamecore`). Add the following to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <relocations>
                            <relocation>
                                <pattern>com.arkflame.core</pattern>
                                <shadedPattern>${project.groupId}.flamecore</shadedPattern>
                            </relocation>
                        </relocations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Ensure `${project.groupId}` is defined in your `pom.xml` (e.g., `com.yourplugin`). This relocates FlameCore's classes (e.g., `com.arkflame.core`) to your plugin's package (e.g., `com.yourplugin.flamecore`).

## API Guides

Detailed usage guides for each FlameCore API are available in the `docs/` directory:

- [ActionBarAPI Getting Started Guide](docs/ActionBarAPI_Getting_Started_Guide.markdown)
- [BlocksAPI Getting Started Guide](docs/BlocksAPI_Getting_Started_Guide.markdown)
- [BossBarAPI Getting Started Guide](docs/BossBarAPI_Getting_Started_Guide.markdown)
- [ColorAPI Usage Guide](docs/ColorAPI_Usage_Guide.markdown)
- [CommandAPI Getting Started Guide](docs/CommandAPI_Getting_Started_Guide.markdown)
- [ConfigAPI Getting Started Guide](docs/ConfigAPI_Getting_Started_Guide.markdown)
- [LangAPI Getting Started Guide](docs/LangAPI_Getting_Started_Guide.markdown)
- [MaterialAPI Getting Started Guide](docs/MaterialAPI_Getting_Started_Guide.markdown)
- [MenuAPI Usage Guide](docs/MenuAPI_Usage_Guide.markdown)
- [MySQLAPI Getting Started Guide](docs/MySQLAPI_Getting_Started_Guide.markdown)
- [SchematicAPI Getting Started Guide](docs/SchematicAPI_Getting_Started_Guide.markdown)
- [SQLiteAPI Getting Started Guide](docs/SQLiteAPI_Getting_Started_Guide.markdown)
- [TitleAPI Getting Started Guide](docs/TitleAPI_Getting_Started_Guide.markdown)

Each guide provides step-by-step instructions and code examples to help you integrate the respective API into your plugin.

## Contributing

Contributions are welcome! Please submit pull requests or open issues on the [GitHub repository](https://github.com/ArkFlame/FlameCore). Ensure code follows the project's style guidelines and includes appropriate tests.

## License

FlameCore is licensed under the [MIT License](LICENSE). See the LICENSE file for details.