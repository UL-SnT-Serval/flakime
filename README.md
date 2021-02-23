# Flakime 
This project allows simulating flakiness by injecting exception during test sources compilation at computed locations.

## Usage
The tool is packaged as a maven plugin and can be used by providing the following plugin info in the target project `pom.xml`.
```xml
<plugin>
    <groupId>lu.uni.serval</groupId>
    <artifactId>flakime-maven-plugin</artifactId>
    <version>{FLAKI_ME_VERSION}</version>
    <executions>
      <execution>
        <goals>
          <goal>flakime-injector</goal>
        </goals>
      </execution>
    </executions>
</plugin>
```

An environment variable should also be set for the instrumentation to be executed : \
`FLAKE_FLAG_FLAKIME` : string boolean. If not found or set to `False` the instrumentation will have no effect on the test suite.


#### Common configuration :
| tag                  | type & range                   | default   | required | description                                                                    |
|----------------------|--------------------------------|-----------|----------|--------------------------------------------------------------------------------|
| `flakeRate`          | float: 0..1                    | 0.05      |          | The threshold at which to consider a test with non-null probability to flake   |
| `strategy`           | string: {vocabulary,bernoulli} | bernoulli |          | The strategy with which the flakiness probability of a test will be calculated |
| `testAnnotations`    | Array of string                |           | X        | The Test annotations used in the test suite preceeded by `@`                   |
| `strategyParameters` | Array of key-value property    | none      |          | The parameters specific to each strategy implementation (see desc. bellow)     |

#### Vocabulary strategy parameters :
| key                   | value type & range | default                                            | required                            | description                                                                                               |
|-----------------------|--------------------|----------------------------------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `trainModel`          | boolean            | true                                               |                                     | Should the model be trained ?                                                                           |
| `modelPath`           | string             |                                                    | X (if `trainModel` is set to false) | The path to the pre-trained serialized model                                                              |
| `randomForestTrees`   | integer : > 0      | 100                                                |                                     | The number of Trees the random forest will be composed of. (No effect if `trainModel` is set to false)    |
| `randomForestThreads` | integer : > 0      | Number of CPU cores available on the machine       |                                     | The number of threads used during the random forest training. (No effect if `trainModel` is set to false) |

#### Bernoulli strategy parameters :

*TBD*

#### Sample configuration :
```xml
<configuration>
  <flakeRate>0.99</flakeRate>
  <strategy>vocabulary</strategy>
  <testAnnotations>
    <annotation>@org.junit.jupiter.api.Test</annotation>
    <annotation>@org.junit.Test</annotation>
  </testAnnotations>
  <strategyParameters>
    <trainModel>false</trainModel>
    <modelPath>rfc_classifier</modelPath>
    <randomForestTrees>20</randomForestTrees>
    <randomForestThreads>12</randomForestThreads>
  </strategyParameters>
</configuration>
```
## Background
### Bernoulli Strategy 
TBD

### Vocabulary Strategy
TBD
