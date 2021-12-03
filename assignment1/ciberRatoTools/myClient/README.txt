### Contents

    Client.java      Source of abstract reactive agent using the Java interface
    ciberIF/         ciberIF package that implements the CiberRato Java Interface
    README.txt	     This README file
    doc/             Documentation in HTML 

### How to use

    * Compilation

    To compile the Client execute the following command from the command line:

       javac Client*.java
       or
       ./build.sh


    * Execution

    To execute the Client, first start the Ciber-Rato simulator from the
    CiberTools and the execute the command:

       java ClientC[1|2|3] --host localhost --pos 1 --robname jClient --filename mapping.out
       or
       java ClientC[1|2|3] -h localhost -p 1 -r jClient -f mapping.out
       or
       ./run.sh -c [1|2|3] -h localhost -p 1 -r jClient -f mapping.out
