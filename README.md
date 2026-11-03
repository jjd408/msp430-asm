# MSP430 Assembly for Apache NetBeans

A NetBeans module for MSP430 and MSP430X assembly in GNU `as` syntax (`msp430-elf-as`, `msp430-elf-gcc`).

## What it does

Opens `.s`, `.S` (C-preprocessed), `.s43` and `.asm` files in an editor with syntax highlighting:

| Colour | For |
| --- | --- |
| Instruction | the 27 core instructions, the emulated ones (`clr`, `inc`, `ret`, ...) and the MSP430X ones (`mova`, `calla`, `pushm`, `rrcm`, `movx`, ...), with `.b`/`.w`/`.a` suffixes |
| Register | `R0`–`R15`, `PC`, `SP`, `SR`, `CG1`, `CG2` |
| Directive | GNU `as` directives (`.text`, `.word`, `.global`, ...) |
| Label | `name:` and local labels (`1:`), at the start of a statement |
| Addressing mode | `#` (immediate), `&` (absolute), `@` (indirect) |
| Number, string, character | `0x1F`, `0b101`, `017`, `0FFh`, local references (`1b`, `1f`); `"text"`; `'A` |
| Comment | `;` to the end of the line, and `//` and `/* */` (for `.S` files) |
| Preprocessor line | `#include`, `#define` and the rest, at the start of a line |

The first word of each statement is its mnemonic, so registers and symbols in the operands aren't mistaken for
instructions. Colours can be changed under **Tools > Options > Fonts & Colors > MSP430 Assembly**. **File >
New File > Other > MSP430 Assembly File** starts a new file.

## Building and installing

Needs JDK 17 or later and Maven. It's built against Apache NetBeans 31 (`RELEASE310` APIs).

```sh
mvn package          # builds target/msp430-asm-1.0-SNAPSHOT.nbm and runs the lexer tests
```

Install the `.nbm` with **Tools > Plugins > Downloaded > Add Plugins...**, then **Install**.

To try it without installing, in a NetBeans with its own throwaway settings (`target/userdir`):

```sh
mvn nbm:cluster nbm:run-ide "-Dnetbeans.run.params=--open some/file.S"
```

`netbeans.installation` in `pom.xml` points at the NetBeans to run (`/usr/lib/apache-netbeans`).

## Layout

| Path | What |
| --- | --- |
| `src/main/java/.../lexer/` | The lexer: `Msp430Lexer`, the instruction set tables (`Msp430Isa`), token kinds |
| `src/main/java/.../file/` | The file type, its editor and its new-file template |
| `src/main/resources/.../` | Colours (`FontsColors.xml`), the Options preview, names (`Bundle.properties`) |
| `src/test/java/.../lexer/` | Lexer tests |

## Licence

GPL-2.0-or-later: the GNU General Public License, version 2 or (at your option) any later version. See
[LICENSE](LICENSE).
