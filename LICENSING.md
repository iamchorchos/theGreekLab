# TheGreekLab component licensing

TheGreekLab is distributed under component-specific licenses.

## 1. Original Java code, tests and documentation

- License: MIT
- Copyright (c) 2026 Olivier Chorchos
- Full text: [`LICENSES/MIT.txt`](LICENSES/MIT.txt)

## 2. Native `pbivnorm` component

Files:

- `src/main/fortran/pbivnorm.f`
- native binaries compiled from that source, including `pbivnorm.dll` and
  `libpbivnorm.so`

Upstream license: GPL-2.0-or-later. The full text is available in
[`LICENSES/GPL-2.0.txt`](LICENSES/GPL-2.0.txt).

## 3. Combined distribution

A JAR, application or other distribution that combines the MIT-licensed Java
component with the GPL-covered `pbivnorm` component is distributed under
GPL-3.0-or-later. The MIT license continues to apply independently to the
original Java files. The full text is available in
[`LICENSES/GPL-3.0.txt`](LICENSES/GPL-3.0.txt).

Third-party Maven dependencies retain their respective licenses. See
[`NOTICE`](NOTICE) for attribution and provenance of the native component.
