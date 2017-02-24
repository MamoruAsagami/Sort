# Sort
Sort is a Unix, Linux, Posix and GNU compatible sort program that runs in Java virtual machine. In addition to command line mode, it has GUI mode.

## 1 How to run
1. From command line shell, type java -jar Sort.jar [arguments]
2. From GUI shell, double-click Sort.jar icon. 

It runs in GUI mode if no arguments are specified.

## 2 Command line arguments

```java
Sort 1.0 - Sort text files
usage: Sort [option ...] [input-file ...]
 -b,--ignore-leading-blanks        ignore leading blanks
    --batch-size <NMERGE>          merge at most NMERGE inputs at once; for more
                                   use temp files
 -c                                check for sorted input; do not sort
 -C                                like -c, but do not report first bad line
    --check <[arg]>                none|diagnose-first: the same as -c,
                                   quiet|silent: the same as -C
    --cli                          run in CLI mode
    --compress-program <PROG>      compress temporaries with PROG; decompress
                                   them with PROG -d.  (Embedded GZIP is used
                                   regardless of PROG)
 -d,--dictionary-order             consider only blanks and alphanumeric
                                   characters
    --debug                        annotate the part of the line used to sort,
                                   and warn about questionable usage to stderr
    --encoding <CHARSET>           use CHARSET to read and write.
                                   CHARSET:=charset|in-charset, out-charset
 -f,--ignore-case                  fold lower case to upper case characters
    --files0-from <F>              read input from the files specified by
                                   NUL-terminated names in file F; If F is '-'
                                   then read names from standard input
 -g,--general-numeric-sort         compare according to general numerical value
 -h,--human-numeric-sort           compare human readable numbers (e.g., 2K 1G)
    --header <n [, every|first]>   n: the number of header lines, every: every
                                   file has header lines, first: only the first
                                   file has header lines.
    --help                         show help message
 -i,--ignore-nonprinting           consider only printable characters
 -k,--key <KEYDEF>                 sort via a key; KEYDEF gives location and
                                   type
    --locale <LOCALE>              use LOCALE for collation; none, default or
                                   language [, country [, variant]].
                                   LOCALE:=locale|text-locale,number-locale
 -M,--month-sort                   compare (unknown) < 'JAN' < ... < 'DEC'
 -m,--merge                        merge already sorted files; do not sort
 -n,--numeric-sort                 compare according to string numerical value
 -o,--output <FILE>                write result to FILE instead of standard
                                   output
    --parallel <N>                 change the number of sorts run concurrently
                                   to N
 -R,--random-sort                  shuffle, but group identical keys.
 -r,--reverse                      reverse the result of comparisons
    --random-source <FILE>         get random bytes from FILE
 -s,--stable                       stabilize sort by disabling last-resort
                                   comparison
 -S,--buffer-size <SIZE>           use SIZE for main memory buffer
    --sort <WORD>                  sort according to WORD: general-numeric -g,
                                   human-numeric -h, numeric -n, month -M,
                                   random -R, version -V
 -t,--field-separator <SEP>        use SEP instead of non-blank to blank
                                   transition. SEP:=c|'c'|'\t'|'/t'
 -T,--temporary-directory <DIR>    use DIR for temporaries, not TMPDIR
 -u,--unique                       with -c, check for strict ordering; without
                                   -c, output only the first of an equal run
 -V,--version-sort                 natural sort of (version) numbers within text
 -z,--zero-terminated              line delimiter is NUL, not newline

KEYDEF is F[.C][OPTS][,F[.C][OPTS]] for start and stop position, where F is a
field number and C a character position in the field; both are origin 1, and
the stop position defaults to the line's end.  If neither -t nor -b is in
effect, characters in a field are counted from the beginning of the preceding
whitespace.  OPTS is one or more single-letter ordering options [bdfgiMhnRrV],
which override global ordering options for that key.  If no key is given, use
the entire line as the key.  Use --debug to diagnose incorrect key usage.

SIZE may be followed by the following multiplicative suffixes:
% 1% of memory, b 1, K 1024 (default), and so on for M, G, T, P, E, Z, Y.

```
The standard options are described in detail, for example, at https://www.gnu.org/software/coreutils/manual/html_node/sort-invocation.html.
In additon to the standard options, the following options are availeble.

 Option   | Arguments | Description
----------|-----------|-------------
\--cli     |        -                   |Forces to run in CLI mode. Use it when there are no other options to specifiy.
\--encoding |in-charset [, out-charset]  |Input and output encodings. Specify only one when they are the same.
\--header  |n [, every&#124;first]        |The number of header lines of every or first file.
\--locale  |text-locale [, number-locale]|Text and number locales. Specify only one when they are the same. Text-locale is used for collation and number-locale is used to decide number format.

## 3 GUI main window
 Element  | Description
----------|-------------
In file   | Input file.  Use the button to pop up a file browser.  You can drop a file icon into the text field, as well.
Out file  | Output file.  Use the button to pop up a file browser.  You can drop a file icon into the text field, as well.
Encoding.Input  | Input file encoding. Automatic means to use heuristic logic to decide the file encoding. Default means the system default file encoding.
Encoding.Output | Output file encoding. Automatic means to use the same encoding as input. Default means the system default file encoding.
Locale.Text   | Text locale to decide text collation.
Locale.Number | Number locale to decide number format (decimal point and thousand separator).
Field separator | A character to separate fields.  Default means zero-length string between non-blank character and blank character. \t for tab code.
Stable | To specify stable sort option.
Unique | To specify unique option which eliminates output lines of the same key values.
Header | The number of header lines.  The header is copied from input to output not sorting.
Buffer size | Internal sort buffer size
Start Field # | Field number where th key starts.  BOL means the beginning of line.
Start Char #  | Character position of the field where the key starts.  Blank means the beginning of the field.
Start Skip blanks  | Specifies to skip blanks to find the key position.
End Field # | Field number where th key ends.  EOL means the end of line.
End Char #  | Character position of the field where the key ends. Blank means the end of the field.
End Skip blanks  | Specifies to skip blanks to find the end of key position.
Sort Kind | Comparison method
Ignore    | Ignore filter
Translate | Translate filter
Reverse   | Reverses the comparison to make it descending sort.
Add       | Adds a new key
Remove    | Removes the key
Up        | Moves up the key
Down      | Moves down the key
Sort      | Starts sorting

