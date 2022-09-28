# XML2CoNLL

This java package can be used to transform xml to conll. We focus on linguistic corpora data, however you may have success extending the template approach to other fields.

## Usage

### In java
In case you want to use the Template converter, you will need a template json file to go with it, that the converter
will try to match to.
The result will be written to the PrintStream object. In case you want to write to a file directly, you may simply use the utility function
`convertFileToPrintStream(File)`.

```java
TemplateXMLConverter txc = new TemplateXMLConverter("Path/To/Template/File");
File xmlFile = ...
File targetFile = ...

txc.getFullCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile));
```

In case you previously know which Template matches best, you can also provide it directly:


```java
Template template = ...
txc.getFullCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile), template);
```

The second way of using our package is through the Generic converter. Note that the result is not proper conll,
as it does not have sentence borders and potentially not a word per line. However, we transform all xml attributes
to a tab separated file that most likely is able to be processed by other CoNLL compatible pipelines.

Again, use the Util function to convert the File to a PrintStream.
```java
GenericXMLConverter txc = new GenericXMLConverter();
File xmlFile = ...
File targetFile = ...
txc.getFullPseudoCoNLL(xmlFile, Utils.convertFileToPrintStream(targetFile));
```

### Commandline interface

This package has two main entry points via commandline interface.

### XML to CoNLL Conversion, based on templates
* Converts known xml dialects into conll, based on templates.
* templates are contained in lib/templates.json and can be added manually
* Usage: `./run.sh XMLConverter -f IN_FILE -t TEMPLATE_PATH [-o OUT_FILE] [-l LENGTH] [-s SAMPLE_SIZE] [--silent]`
  * IN_FILE: XML file to convert
  * TEMPLATE_PATH: path to template json
  * OUT_FILE: default std out, where to write converted conll
  * LENGTH: default 999, how many sentences to convert
  * SAMPLE_SIZE: default 500, How many nodes to sample
  * --silent: no logging output (also not this synopsis!)
* Sample usage: `./run.sh XMLConverter -f tinytest.xml -t lib/templates.json`



### Generic XML Recognition
* Finds potential linguistic annotation within xml files. provide an xml file
* Usage: `./run.sh GenericXMLRecognizer -f FILE [-s SAMPLE_SIZE] [-t THRESHOLD] [--silent]`
  * FILE: XML file to check
  * SAMPLE_SIZE:  default 500, How many nodes to sample
  * THRESHOLD: default 0.6, float what percentage of checks should pass
  * --silent: no logging output (also not this synopsis!)
* Sample usage: `./run.sh GenericXMLRecognizer -f tinytest.xml`


