## Introduction

A general documentation is available here : https://docs.google.com/document/d/18jevlPr6iy0V4eQ8OYvIq_bCqD5LeNuvTifkIqD8TnM/edit?usp=sharing

A Guide prototyping will be available soon.

## Principles
Balsamiq is a wireframe tool to easily create screen mockups. Gabbro is designed to allow rapid generation of source code from the definition of the balsamiq model file. Code generation is performed by freemarker templates. The mockup of the screen must respect certain rules, in particular the concept of container and alignment of different widgets. The mockup guide details the different rules. Gabbro reconstructs a hierarchy of different mockups and recalculates the position of each in relation to the father widget container. The position of each widget is also calculated in twelfth with an offset relative to the beginning of the line. (same as bootstrap)
