# MSP430 Assembly for Apache NetBeans

A NetBeans module for MSP430 and MSP430X assembly in GNU `as` syntax (`msp430-elf-as`, `msp430-elf-gcc`).

## What it does

### Machine code beside the source

A column between the line numbers and the text shows what each line assembles to: its offset in its section
and its machine code as 16-bit words, the way the CPU reads them (`mov.w #0x5A80, &0x01CC` → `0000  40B2 5A80
01CC`). Data directives show their bytes the same way. Long data is cut short in the column; hover for all of
it. Fields the linker fills in (symbols such as `WDTCTL` from TI's headers, external labels) show as `0000`.

The file is assembled in the background with `msp430-elf-as -mmcu=msp430fr2355` whenever you stop typing for a
moment, unsaved changes included. `.S` files go through the C preprocessor first, with TI's support headers
(`#include <msp430.h>`) and the file's own folder on the include path. Assembler and preprocessor errors and
warnings are marked on their lines, with the usual NetBeans error icon, underline and stripe. Errors don't cost
the other lines their addresses: the file is assembled again without the lines in error to get them.

The toolchain is found in `~/ti/msp430-gcc/bin`, `/opt/ti/msp430-gcc/bin`, `/usr/local/msp430-gcc/bin` or on the
PATH. (Settings for its folder and the `-mmcu` device are stored as the module's `toolchainBin` and `mcu`
preferences; an Options page for them is to come.)

### Completion and help

**Ctrl+Space** offers what fits where the caret is, with help for the selected choice beside the list:

| Where | Offered |
| --- | --- |
| The start of a statement (after any label) | Instructions, in each size they take (`mov`, `mov.b`, `mov.w`; `movx.a`), or directives after a `.` |
| After `&` (absolute) | The device's peripheral registers (`P1OUT`, `WDTCTL`, ...), with their sizes and addresses; labels |
| After `#` (immediate) | The device's constants (`BIT0`, `WDTPW`, `TBSSEL__ACLK`, ...), with their values and registers; labels |
| After `@` (indirect) | CPU registers |
| Other operands | CPU registers and labels; peripheral registers and constants once two letters narrow them down |

The list opens by itself after typing `&` or `#`. Instruction help gives the syntax, the operation, which status
bits (V N Z C) it changes, its sizes, and what an emulated instruction assembles as. Peripheral registers and
constants come from TI's own support files for the device (`msp430fr2355.h` and `msp430fr2355_symbols.ld`), so
they match the headers your code includes.

**Hold Ctrl and hover** over an instruction, directive, register, peripheral register, constant or label for the
same help; **Ctrl+click** a label to go to where it's defined.

### Syntax highlighting

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
| `src/main/java/.../assemble/` | Running the toolchain and reading its listing, cpp line markers and messages (plain Java) |
| `src/main/java/.../machinecode/` | The machine code column and the per-document model that re-assembles and marks errors |
| `src/main/java/.../help/` | Instruction, directive and register reference; the device's registers and constants from TI's files |
| `src/main/java/.../completion/` | Completion (what's offered where, in `Context` and `Choices`) and Ctrl+hover help |
| `src/main/resources/.../` | Colours (`FontsColors.xml`), the Options preview, names (`Bundle.properties`) |
| `src/test/java/.../lexer/` | Lexer tests |
| `src/test/java/.../assemble/` | Listing, line map and message parsing; assembling with the real toolchain (skipped without it) |
| `src/test/java/.../help/`, `.../completion/` | Help coverage, TI's files, completion contexts and choices, hover help |

## Licence

GPL-2.0-or-later: the GNU General Public License, version 2 or (at your option) any later version. See
[LICENSE](LICENSE).
