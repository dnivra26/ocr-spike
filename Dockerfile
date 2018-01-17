FROM openjdk:8u102-jdk
ADD build/libs/ocr-spike-0.1.0.jar ocr-spike-0.1.0.jar
CMD java -jar ocr-spike-0.1.0.jar
