# calclang
This repo is now deprecated, in favor of the *much* faster, vastly improved
[CalcLang2](https://github.com/calclang/calclang2).

![calclang](/screenshots/geometry_output.png?raw=true "calclang")

[Examples](#example)

# calclang
A pointless scripting language that does math. Made with ANTLR 4 and Java. **Terrible docs.**

# Types
The only type is `Double`. They can be negative, and you may omit the decimals.
Hexadecimal is accepted too.
```
    0x10 becomes 16.0
    3 becomes 3.0
    3.5 because 3.5
```

# Keywords
* else
* end
* fn
* if
* inc
* loop
* print
* ret

# Strings
In a word, **no**.
However, you can print.
ES6-like string interpolation is allowed. Just include an expression.
Yes, you can perform arithmetic, call functions, or resolve variables within
strings.

```
    print "Not another hello world..."
    print "${2 + 2} = 4"
```

# Boolean
Booleans in Calclang are mapped to numbers.

```
    true becomes 1.0
    false becomes 0.0
    1.0 == true
    0.0 == false

    if (true)
        print "Hello, bool!"
    end if
```

# "Standard Lib"
* Predefined Constants:
    * PI
    * AVOGADRO
    * PLANCK
    * SPEED_OF_LIGHT
    * E
    * I
    * INFINITY
* Predefined Functions:
    * All of these guys:
    * ![Predefined Functions](/screenshots/stdfuncs.png?raw=true "Predefined Functions")
    * rnd is Math.random
    * root(x, n) - Return the nth root of x
    * log(x, base) - logarithm with base other than E or 10
    * rad/deg - Convert to rad/deg
    * **avg** Returns the average of all arguments

# Usage
1.  Build the jar, or compile the source and run the classes.
2.  Run it with the option -h.
3.  With no options, you will enter... REPL mode. Which, of course, is broken.
4.  *Not all of the examples work yet... i.e. Fibonacci*

# Build

To reproduce...

> mvn clean install

# Stuff
*   You must have an `fn main()` block for anything to happen.

## Variables
* Only can declare variables within functions.
* If you want to do it globally, prefix with "global"

```
global one = 1

fn hi()
    two = 2
    print "one=${one}, two=${two}"
end fn
```

## Functions
* No nesting yet

```
// Can have 0 or more params. The parentheses() are required.
// You must have a return statement.
fn sinFromDegrees(deg)
    ret sin(rad(deg))
end sinFromDegrees
```

## Includes
* Only at the top of the file

```
    inc "./myfile.calc"
```

## Loop
* No for statements yet, sorry

```
times = 5000
loop times
    print "I like it!"
end loop
```

# Example

Math Samples
![Math](/screenshots/math.png?raw=true "Math")

Mass Stoichiometry
![Stoichiometry](/screenshots/stoichiometry_source.png?raw=true "Stoichiometry")

Geometry, Printing
![Example](/screenshots/geometry_source.png?raw=true "Example")

Fibonacci
![Fib](/screenshots/fibonacci.png?raw=true "Fib")
![Fib](/screenshots/fibonacci_output.png?raw=true "Fib")
