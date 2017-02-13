The Java source files are in package practicum and package adapter.

(after filling in the blanks) it can be build using ant using the command

	ant

(see  ant -projecthelp  for available targets)


the result can be run using as follows.

to run the puzzle program with 'human interface':

	java -jar practicum.jar


to run the puzzle program with 'non-human interface' via the adapter:

	java -jar adapter.jar java -jar practicum.jar -t

