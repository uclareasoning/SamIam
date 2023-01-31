call javacc -STATIC:false NeticaReader.jj
del TokenMgrError.java
del ParseException.java
del Token.java
del SimpleCharStream.java