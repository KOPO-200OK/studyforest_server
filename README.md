# studyforest
studyforest

## Backend local setup

- Runtime: JDK 21, Spring Boot 3.4.13, Gradle 8.x
- Profiles: `univ`, `cloud`, `test`
- Copy the required section of `application-example.yml` to an ignored
  `application-{profile}.yml`, then provide every value through environment variables.
- Never commit database credentials, JWT secrets, API keys, or Oracle Wallet files.

```bash
./gradlew clean test
./gradlew bootRun --args='--spring.profiles.active=univ'
./gradlew bootRun --args='--spring.profiles.active=cloud'
```

Backend API, DTO, package, ERD, and table conventions are documented in
[`docs/backend-conventions.md`](docs/backend-conventions.md).
