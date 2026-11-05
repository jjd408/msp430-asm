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

The toolchain and device are set in Options (below).

### Build and run

**Build MSP430 Hex** (in the Build menu, the toolbar, and the file's and editor's right-click menus) saves the
file and builds it with `msp430-elf-gcc` for the device, linked with TI's linker script and gcc's start-up code,
so the program starts at a global `main`. The `.elf` and an Intel HEX `.hex` go in a `build` folder beside the
source. The build's commands, messages and size table appear in the Output window; click a line that names the
source to go to it. Assembler errors and the linker's "undefined reference" errors land on the right line.

**Run in PICSimLab** (Run menu, toolbar, right-click menus) builds, then starts PICSimLab with the hex on the
**Solitaire** board (the handheld's display and buttons) or the bare **MSP430_Board**. It needs PICSimLab with
the MSP430 backend, [jjd408/picsimlab-msp430](https://github.com/jjd408/picsimlab-msp430); its first start takes a
few seconds while `run.sh` builds it.

### Projects

A program of several files is an **MSP430 project**: **File > New Project > MSP430 > MSP430 Project** asks for a
name and folder and makes one with a starter program in two files: `src/main.S` blinks P1.0 and calls `delay` in
`src/delay.S`. Any folder with an `msp430-project.properties` file opens as one (**File > Open Project**).

```
Blinky/
  msp430-project.properties   name, and optional mcu, cflags, board
  src/                        every .s .S .s43 .asm .c here (and in folders below) is built
  build/Blinky.elf, .hex      the program
```

The project's **Build** (F11), **Clean and Build**, **Clean** and **Run** (F6, in PICSimLab) build everything in
`src/` into one program; files include each other from `src/`. Errors in any file are clickable in the Output
window. The properties file overrides Options for that project:

| Key | What | Empty |
| --- | --- | --- |
| `name` | Program name, for `build/<name>.hex` | the folder's name |
| `mcu` | Device for `-mmcu` | the Options device |
| `cflags` | Extra compiler flags, for C files | none (new projects get `-Os -Wall`) |
| `board` | PICSimLab board | the Options board |

### Debugging

**Debug in PICSimLab** (Run menu, toolbar, the file's and editor's right-click menus) or, in a project, **Debug
Project** (Ctrl+F5) builds the program and debugs it on the PICSimLab board:

- Click in the editor's left margin (or **Ctrl+F8**) to set a breakpoint on a line; click again to remove it.
  They're listed in **Window > Debugging > Breakpoints** as `main.S:12` (disable, delete, double-click to go
  there), follow their lines as you edit, and are kept between sessions. Breakpoints in C files come from
  NetBeans' C/C++ support and work the same way.
- The program stops at a breakpoint with its line marked green. **Continue** (F5), **Step Over** (F8), **Step
  Into** (F7), **Step Out** (Ctrl+F7) and **Pause** work from the Debug menu and toolbar; **Finish Debugger
  Session** (Shift+F5) leaves the program running in PICSimLab.
- **Variables** shows the CPU registers (SR's flags spelled out) and every peripheral register of the device by
  name, grouped by module (P1, TB0, UCB1, WDT...), with TI's descriptions as tooltips. Reading them doesn't
  disturb the program (no flags cleared).
- **Watches** take any expression gdb can evaluate: C variables, `$r12`, `*(unsigned char *)0x0202`,
  `*(unsigned *)&counter` for an assembly label.

How it works: the plugin starts PICSimLab with debugging on (`run.sh --debug`), whose MSP430 backend serves
GDB's protocol on port 1234, and drives TI's `msp430-elf-gdb` (through picsimlab-msp430's `tools/msp430-gdb`,
which lets it run with ncurses 6) over its machine interface. If PICSimLab is already debugging on that port,
the session loads the new program into it and resets the chip, so there's no need to restart it. While the
program is stopped, the whole board waits. The session's messages go to the build's Output tab; for gdb's whole
conversation, start NetBeans with `-J-Dio.github.jjd408.msp430asm.debug.Gdb.level=FINE` (it goes to
`messages.log`).

### Options

**Tools > Options > Miscellaneous > MSP430**:

| Setting | Default |
| --- | --- |
| MSP430-GCC bin folder | found in `~/ti/msp430-gcc/bin`, `/opt/ti/msp430-gcc/bin`, `/usr/local/msp430-gcc/bin` or on the PATH |
| Device (`-mmcu`) | `msp430fr2355`; it picks the linker script and the headers completion reads |
| PICSimLab folder | the `picsimlab-msp430` checkout with `run.sh`; found in `~/projects/solitaire/sim/picsimlab-msp430` |
| PICSimLab board | Solitaire |

The page shows which toolchain and PICSimLab it found.

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

### Documentation window

**MSP430 Documentation** (beside the editor; **Window > MSP430 Documentation** or the editor's right-click menu)
explains what's under the cursor as you move it: an instruction (operation, flags, sizes, what an emulated one
assembles as), a directive, a CPU register, a peripheral register with its bits, or a constant. With the cursor
elsewhere on an instruction's line (between operands, in the comment), it shows that line's instruction. It opens
by itself the first time you edit an assembly file; after that it stays where you leave it, closed or open.

### Documentation

**Help > MSP430 Documentation** opens TI's guides in the web browser: the MSP430FR4xx/FR2xx family user's guide
(SLAU445: the CPU, instruction set and every peripheral), the MSP430FR2355 datasheet (pins, memory map,
electrical limits), the MSP430-GCC user's guide (SLAU646), and the GNU assembler manual's MSP430 section.

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
mvn package          # builds target/msp430-asm-1.0-SNAPSHOT.nbm, runs the tests, and updates the update center
```

### Updating from a local update center (once set up, no reinstalling)

Every build gets a higher version (`1.0.<date>.1<time>`, UTC) and writes an update center to
`target/netbeans_site/updates.xml`. Add it to NetBeans once:

1. **Tools > Plugins > Settings > Add**
2. Name: `MSP430 Assembly (local build)`; URL: `file:/home/joe/NetBeansProjects/msp430-asm/target/netbeans_site/updates.xml`
3. **OK**

Then after each `mvn package`: **Tools > Plugins > Updates > Check for Updates**, tick **MSP430 Assembly**,
**Update**. NetBeans replaces the installed copy (restarting itself if it asks to). A first install can come
from the same place (**Available Plugins**), or from the `.nbm` with **Downloaded > Add Plugins...**.

(`mvn clean` deletes the update center until the next `mvn package`; NetBeans just reports it unreachable meanwhile.)

### Trying it without installing

In a NetBeans with its own throwaway settings (`target/userdir`):

```sh
mvn nbm:cluster nbm:run-ide
/usr/lib/apache-netbeans/bin/netbeans --userdir target/userdir --open some/file.S   # while it runs
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
| `src/main/java/.../assemble/Build.java`, `PicSimLab.java` | Building to .elf/.hex and starting PICSimLab (plain Java) |
| `src/main/java/.../build/` | The Build and Run actions and their Output window |
| `src/main/java/.../options/` | The Options page |
| `src/main/java/.../debug/` | Debugging: breakpoints (gutter toggle, marks, Breakpoints window, saving them), gdb's MI (`Mi`, `Gdb`), the session (`Msp430Debugger`), its actions and windows (`VariablesModel`, `WatchesModel`) |
| `src/main/java/.../project/` | The MSP430 project type: recognizing it, its node and actions, the New Project wizard; `ProjectFiles` is plain Java |
| `src/main/resources/.../` | Colours (`FontsColors.xml`), the Options preview, names (`Bundle.properties`) |
| `src/test/java/.../lexer/` | Lexer tests |
| `src/test/java/.../assemble/` | Listing, line map and message parsing; assembling with the real toolchain (skipped without it) |
| `src/test/java/.../help/`, `.../completion/` | Help coverage, TI's files, completion contexts and choices, hover help |
| `src/test/java/.../assemble/BuildTest.java` | Building a hex, error and linker-error lines, the PICSimLab command |
| `src/test/java/.../debug/` | Breakpoints, MI parsing, a debugging session against the simulator (`msp430_run --gdb`, skipped without it), the Variables window |
| `src/test/java/.../project/` | Making a project, which files are sources, building the starter with a C file, errors per file |

## Licence

GPL-2.0-or-later: the GNU General Public License, version 2 or (at your option) any later version. See
[LICENSE](LICENSE).
