# Changelog

### [1.0.17](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.16...v1.0.17) (2021-07-13)


### Bug Fixes

* (rest transport) Add `@BetaApi` to the generated `TransportServiceFactory` class and lro-specific method ([#787](https://www.github.com/googleapis/gapic-generator-java/issues/787)) ([ebe1aef](https://www.github.com/googleapis/gapic-generator-java/commit/ebe1aefbe860a41aabd8ced5268ccc6c7efdf791))

### [1.0.16](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.15...v1.0.16) (2021-06-30)


### Bug Fixes

* **bazel:** Eradicate monolith deps from Java µgen repo ([#778](https://www.github.com/googleapis/gapic-generator-java/issues/778)) ([86f2472](https://www.github.com/googleapis/gapic-generator-java/commit/86f2472963f020127ef6ff92be6241d12e2273af))
* **build:** Update googleapis-discovery hash to fix compute integration test ([#782](https://www.github.com/googleapis/gapic-generator-java/issues/782)) ([46bb19a](https://www.github.com/googleapis/gapic-generator-java/commit/46bb19a7ee61da86b4be6d87a71f3cd210e753d2))
* **protoc:** Mirror protoc's field name conflict resolution logic in client generation ([#781](https://www.github.com/googleapis/gapic-generator-java/issues/781)) ([9432979](https://www.github.com/googleapis/gapic-generator-java/commit/9432979bab59f48c8645fa47d752cdd470d4a682))

### [1.0.15](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.14...v1.0.15) (2021-06-22)


### Features

* Implement field presence support for DIREGAPIC ([#774](https://www.github.com/googleapis/gapic-generator-java/issues/774)) ([c820361](https://www.github.com/googleapis/gapic-generator-java/commit/c82036105d299b0a1192cd0def5e68253e4f542c))


### Bug Fixes

* **service.yaml:** Remove allowlist restriction ([#776](https://www.github.com/googleapis/gapic-generator-java/issues/776)) ([8f42efd](https://www.github.com/googleapis/gapic-generator-java/commit/8f42efdb92d606a768a524517fe949c4f9112025))


### Miscellaneous Chores

* release 1.0.15 ([f752478](https://www.github.com/googleapis/gapic-generator-java/commit/f75247845344540a94c4efcd416f34f96ea0c2a3))

### [1.0.14](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.13...v1.0.14) (2021-06-17)


### Features

* Add DIREGAPIC-specific pagination ([#767](https://www.github.com/googleapis/gapic-generator-java/issues/767)) ([1294c29](https://www.github.com/googleapis/gapic-generator-java/commit/1294c298f50cc4474ae562e6a07f37a5f94fe5b8))


### Bug Fixes

* **bazel:** Remove monolith rule deps from the Java µgen Bazel rules ([#764](https://www.github.com/googleapis/gapic-generator-java/issues/764)) ([bff3efc](https://www.github.com/googleapis/gapic-generator-java/commit/bff3efc25e43692ea5b6e769c20d25d5b9a1e3d2))



### [1.0.13](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.12...v1.0.13) (2021-06-16)


### Bug Fixes

* **resnames:** Fix resname builder name conflicts in ctor with this assignment ([#769](https://www.github.com/googleapis/gapic-generator-java/issues/769)) ([edac844](https://www.github.com/googleapis/gapic-generator-java/commit/edac8447d74c43ab0db963a37f66e1029ab19f0c))

### [1.0.12](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.11...v1.0.12) (2021-06-10)


### Features

* add mtls support ([#672](https://www.github.com/googleapis/gapic-generator-java/issues/672)) ([1e24893](https://www.github.com/googleapis/gapic-generator-java/commit/1e24893a65daf8ef067e331364c591ac973b5e02))


### Bug Fixes

* **mocks:** Use java.lang.Object if there are protos named 'Object' ([#760](https://www.github.com/googleapis/gapic-generator-java/issues/760)) ([2a7064b](https://www.github.com/googleapis/gapic-generator-java/commit/2a7064b88fe26586bd8aed43b7a7d28c7e974ec0))
* **resnames:** Use anon resname classes when only wildcards are present ([#763](https://www.github.com/googleapis/gapic-generator-java/issues/763)) ([f0ecead](https://www.github.com/googleapis/gapic-generator-java/commit/f0ecead9f1cc645cdbb7f61cdfc820c7df95355d))


### Miscellaneous Chores

* release 1.0.12 ([02eab0e](https://www.github.com/googleapis/gapic-generator-java/commit/02eab0ec61260048a2684119cfd4fa2172f3a637))

### [1.0.11](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.10...v1.0.11) (2021-06-07)


### Features

* DIREGAPIC initial implementation ([#746](https://www.github.com/googleapis/gapic-generator-java/issues/746)) ([81f6737](https://www.github.com/googleapis/gapic-generator-java/commit/81f6737359ac6ce5ee2b42ab4f755fbb05a3cf28))


### Bug Fixes

* **bazel:** ensure integration tests diff files recursively ([#745](https://www.github.com/googleapis/gapic-generator-java/issues/745)) ([ddc75f9](https://www.github.com/googleapis/gapic-generator-java/commit/ddc75f9f3b84d0ea50638a79a63d40cf551211e2))
* **resnames:** filter out mixin services from resname pkg candidates ([#751](https://www.github.com/googleapis/gapic-generator-java/issues/751)) ([72fa76f](https://www.github.com/googleapis/gapic-generator-java/commit/72fa76f27379a74a143b1735f60ae3e00da4c1a6))
* **tests:** Ensure deterministic field ordering in test classes ([#743](https://www.github.com/googleapis/gapic-generator-java/issues/743)) ([fdb705b](https://www.github.com/googleapis/gapic-generator-java/commit/fdb705b0a39443fb0b7679d879f27e0aa1c36b67))
* **tests:** handle Java 11 set ordering differences for RPCs and fields in test/mock classes ([#750](https://www.github.com/googleapis/gapic-generator-java/issues/750)) ([eaf4592](https://www.github.com/googleapis/gapic-generator-java/commit/eaf4592e139fbc42810e7f60dc9967320195bf85))


### [1.0.10](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.9...v1.0.10) (2021-05-26)


### Miscellaneous Chores

* Pre-DIREGAPIC refactoring ([04c2faa](https://github.com/googleapis/gapic-generator-java/commit/04c2faa191a9b5a10b92392fe8482279c4404803))

### [1.0.9](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.8...v1.0.9) (2021-05-26)


### Features

* add AST support for lambdas ([#736](https://www.github.com/googleapis/gapic-generator-java/issues/736)) ([9ced678](https://www.github.com/googleapis/gapic-generator-java/commit/9ced6780c7a6b9934dd548002602618566a539d6))


### Bug Fixes

* Support enums in proto HTTP annotations ([#733](https://www.github.com/googleapis/gapic-generator-java/issues/733)) ([f5c7b86](https://www.github.com/googleapis/gapic-generator-java/commit/f5c7b86b43ad71ffe47d8ba039155db601638e1f))
* Support PubSub's _deleted-topic_ pattern ([#739](https://www.github.com/googleapis/gapic-generator-java/issues/739)) ([7d8c62d](https://www.github.com/googleapis/gapic-generator-java/commit/7d8c62d8e8922a3589e631299ecb0287bc41ba2d))


### [1.0.8](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.7...v1.0.8) (2021-05-24)


### Miscellaneous Chores

* DIREGAPIC refactoring ([ae17e7e](https://github.com/googleapis/gapic-generator-java/commit/ae17e7e9a272b422176962d896e387496b1806e8))

### [1.0.7](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.6...v1.0.7) (2021-05-21)


### Bug Fixes

* Add PubSub to service.yaml / mixin allowlist ([#729](https://www.github.com/googleapis/gapic-generator-java/issues/729)) ([e7f6d33](https://www.github.com/googleapis/gapic-generator-java/commit/e7f6d33051e335504b05c402d3b98c387a9f0daf))


### [1.0.6](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.5...v1.0.6) (2021-05-19)


### Bug Fixes

* **mixins:** Gate mixin RPC on HTTP rules, add yaml doc/http overrides ([#727](https://www.github.com/googleapis/gapic-generator-java/issues/727)) ([229da5d](https://www.github.com/googleapis/gapic-generator-java/commit/229da5d94cf7db060abf3ea006a20d1ade804597))

### [1.0.5](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.4...v1.0.5) (2021-05-17)


### Bug Fixes

* **types:** Reorder nested types for legacy protos w/ java_outer_class in one file ([#724](https://www.github.com/googleapis/gapic-generator-java/issues/724)) ([925356d](https://www.github.com/googleapis/gapic-generator-java/commit/925356d659aed4b8550ce526f1772a706661c246))

### [1.0.4](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.3...v1.0.4) (2021-05-13)


### Bug Fixes

* **types:** Use fully-qualified message type names ([#723](https://www.github.com/googleapis/gapic-generator-java/issues/723)) ([8a5c36c](https://www.github.com/googleapis/gapic-generator-java/commit/8a5c36ccce7540940ec7a4bf8751971c3741d89d))
* Use the right composers in deprecation unit tests ([#715](https://www.github.com/googleapis/gapic-generator-java/issues/715)) ([2318136](https://www.github.com/googleapis/gapic-generator-java/commit/2318136e49060c3212abfd6337e2c3ceb2c2fc69))

### [1.0.3](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.2...v1.0.3) (2021-04-07)


### Bug Fixes

* Comment typo in Parser.java ([#701](https://www.github.com/googleapis/gapic-generator-java/issues/701)) ([685feff](https://www.github.com/googleapis/gapic-generator-java/commit/685feff32bf16484895ed86121282360b3e2dab3))

### [1.0.2](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.1...v1.0.2) (2021-03-14)


### Bug Fixes

* add request with default value in regular paged callable method ([#690](https://www.github.com/googleapis/gapic-generator-java/issues/690)) ([cc8ce77](https://www.github.com/googleapis/gapic-generator-java/commit/cc8ce778f07577eb3ed4cc9d07ac25511b0f1acd))
* **mixins:** handle unit tests for mixin pagination methods ([#691](https://www.github.com/googleapis/gapic-generator-java/issues/691)) ([edd7443](https://www.github.com/googleapis/gapic-generator-java/commit/edd7443d16e5c9389de16e6235fe884f9b996cf6))
* **sample code:** Update client samples in  KMS goldens ([#697](https://www.github.com/googleapis/gapic-generator-java/issues/697)) ([3777196](https://www.github.com/googleapis/gapic-generator-java/commit/3777196f0225a8b28203f1c225a67ea56b8728d3))

### [1.0.1](https://www.github.com/googleapis/gapic-generator-java/compare/v1.0.0...v1.0.1) (2021-03-03)


### Bug Fixes

* **codegen:** Bump license year in generated file headers ([#685](https://www.github.com/googleapis/gapic-generator-java/issues/685)) ([d1b3032](https://www.github.com/googleapis/gapic-generator-java/commit/d1b3032d54bc75e2f4e1b954f7215cff1069110e))
* **metadata:** gate metadata file-gen on a CLI flag ([#684](https://www.github.com/googleapis/gapic-generator-java/issues/684)) ([738bf8a](https://www.github.com/googleapis/gapic-generator-java/commit/738bf8a95125cbdd33cb0f762afb415844bf9426))
* **mixins:** enable codegen for standalone mixin APIs, add IAM integ test ([#679](https://www.github.com/googleapis/gapic-generator-java/issues/679)) ([bbde184](https://www.github.com/googleapis/gapic-generator-java/commit/bbde184b4d5a6085a6f18fd8120bd79207f67c5d))
* **mixins:** enable RPC overrides to clobber mixed-in RPCs ([#678](https://www.github.com/googleapis/gapic-generator-java/issues/678)) ([0645de4](https://www.github.com/googleapis/gapic-generator-java/commit/0645de476d131be20839f74ba83b53483d0d0b6d))

## [1.0.0](https://www.github.com/googleapis/gapic-generator-java/compare/v0.0.21...v1.0.0) (2021-03-01)


### Bug Fixes

* add common srcs to test, check test targets only in codecov ([#669](https://www.github.com/googleapis/gapic-generator-java/issues/669)) ([f81ed0b](https://www.github.com/googleapis/gapic-generator-java/commit/f81ed0bdede477c51cd6755b5050933319a442c9))
* **release:** update gax-java to 1.62.0 ([#675](https://www.github.com/googleapis/gapic-generator-java/issues/675)) ([51ed181](https://www.github.com/googleapis/gapic-generator-java/commit/51ed181f9fa2747604054214db79e1f2398b1dac))
