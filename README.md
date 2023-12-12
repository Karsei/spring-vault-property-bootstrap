# spring-vault-property-bootstrap

Spring Boot 초기 로드 시에 Vault 에서 Secret 데이터를 가져와 임의로 설정된 Mapping 에 따라 Property 를 할당해줍니다.

## 사용법

시스템 환경 변수에 아래 정보 입력

**Windows**

`Win + R` 키 누르고 `rundll32.exe sysdm.cpl,EditEnvironmentVariables` 입력 후 엔터

아래 변수 추가

* VAULT_ROLE_ID
* VAULT_SECRET_ID
* VAULT_ENGINE_NAME

**Linux/Mac**

```shell
# LINUX/MAC 기준
export VAULT_ROLE_ID=***********
export VAULT_SECRET_ID=***********
export VAULT_ENGINE_NAME=***********
```

`.bash_profile` 또는 `.zshrc`(zShell 사용할 경우) 에서 위에 스크립트 추가 후 `source` 명령어로 적용

아래는 예시

```shell
# zshell
$ source ~/.zshrc
# bash
$ source ~/.bash_profile
```

### 설정 변수

| 변수                         | 설명                                                | 기본값   |
|----------------------------|---------------------------------------------------|-------|
| enabled                    | autoconfigure 실행 여부                               | true  |
| uri                        | vault uri                                         |       |
| role-id                    | role id                                           |       |
| secret-id                  | secret id                                         |       |
| engine-name                | 시크릿 엔진 이름                                         |       |
| fail-fast                  | 설정 중 오류로 실패할 경우 즉시 어플리케이션 종료 여부                   | true  |
| request-connect-timeout    | vault 와 연결 지연 허용 시간(밀리초)                          | 10000 |
| property-map               | vault secret 과의 맵핑 정보                             |       |
| override-system-properties | spring.cloud.config.override-system-properties 참고 | false |
| override-none              | spring.cloud.config.override-none 참고              | true  |
| allow-override             | spring.cloud.config.allow-override 참고             | true  |

### property-map

예를 들어,
Vault 의 `some-engine/some/secret` Secret 데이터에서
`hostname`, `port`, `password`, `user` 를 각각
`karsei.mysql.userdb.hostname`, `karsei.mysql.userdb.port`, `karsei.mysql.userdb.password`, `karsei.mysql.userdb.username` 에 맵핑하고 싶으면

* resources/bootstrap.yml 에서

```yaml
vault-property:
  engine-name: some-engine
  property-map:
    "[some/secret]":   # 큰 따옴표(") 와 대괄호([, ]) 로 슬래쉬(/) 이스케이프
      hostname: "karsei.mysql.userdb.hostname"
      port: "karsei.mysql.userdb.port"
      password: "karsei.mysql.userdb.password"
      user: "karsei.mysql.userdb.username"
```
