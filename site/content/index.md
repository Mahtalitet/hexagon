title=Hexagon
date=2016-04-13
type=page
status=published
~~~~~~


HEXAGON ${projectVersion}
=========================
### The atoms of your platform

Hexagon is a micro services framework that doesn't follow the flock. It is written in [Kotlin] and
uses [Ratpack], [Jackson], [RabbitMQ] and [MongoDB]. It takes care of:

* rest
* messaging
* serialization
* storage
* events
* configuration
* logging
* scheduling

The purpose of the project is to provide a micro services framework with the following priorities
(in order):

* Simple to use
* Easily hackable
* Be small

DISCLAIMER: The project status right now is alpha... You should not use it in production yet


## Getting Started

Get the dependency from [JCenter] (you need to setup de repository first):

Gradle:

```groovy
compile ('co.there4:hexagon:${version}')
```

Maven:

```xml
<dependency>
  <groupId>co.there4</groupId>
  <artifactId>hexagon</artifactId>
  <version>${version}</version>
</dependency>
```

[JCenter]: https://bintray.com/jamming/maven/Hexagon

Write the code:

```java
import co.there4.hexagon.rest.ratpack.*

fun main(args: Array<String>) {
    serverStart {
        handlers {
            get("hello/:name") { render("Hello ${pathTokens["name"]}!") }
        }
    }
}
```

Launch it and view the results at: [http://localhost:5050/hello]


## Examples

Check out and try the examples in the source code.

<!---
[Code Examples][Examples]
[Examples]: https://github.com/jamming/hexagon/tree/master/src/test/kotlin/hexagon/Examples.kt

You can also check the [integration tests][tests]
[tests]: https://github.com/jamming/hexagon/tree/master/src/test/kotlin/hexagon/it/undertow
-->