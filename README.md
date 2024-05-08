# ETA E-Invoice Signer

## Description

- A web application for signing e-invoice documents in accordance with
  the [algorithm](https://sdk.invoicing.eta.gov.eg/signature-creation) specified by the Egyptian Tax Authority (ETA).

### Requirements

- JDK 17
    - Oracle JDK 17 can be downloaded
      from [here](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html).

### Building

  ```console
  ./mvnw clean package
  ```

### Configuration

- Configuration properties should be placed in the `application.properties`

##### Hardware Token Keystore

- `signature.keystore.pkcs11ConfigFilePath`
- `signature.keystore.password`
- `signature.keystore.certificateIssuerName`

#### Authentication

- The application uses HTTP Basic authentication.
- Only 1 user can be defined.
- To configure the user's details:
    - Set `auth.user.userName` to the username.
    - Set `auth.user.password` to the password.
