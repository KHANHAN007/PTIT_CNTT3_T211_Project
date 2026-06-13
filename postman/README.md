# Rikkei Bank Postman Test Suite

## Import

Import these two files into Postman:

- `Rikkei-Bank-Test-Suite.postman_collection.json`
- `Rikkei-Bank-Test.postman_environment.json`

Select the **Rikkei Bank - Test Environment** environment before sending requests.

The default `baseUrl` is `http://localhost:8080` for direct IntelliJ execution. Change it to
`http://localhost:8081` when the API is running through Docker Compose.

## Run Order

Run folders in numeric order:

1. Public Registration
2. Authentication
3. Admin User Management
4. Admin Account Management
5. Staff Operations
6. Customer Banking

Authentication requests automatically store JWT access tokens and encrypted opaque refresh tokens. Create/list requests
automatically store IDs needed by later requests.

Before each registration, the collection automatically creates a synchronized unique username, email, and phone number.
It then clears tokens and IDs belonging to the previous customer so later requests cannot accidentally test stale data.
Set `autoGenerateIdentities` to `false` when you want to enter customer identity variables manually.

## Manual Test Inputs

- Select a non-empty PNG/JPEG file for each multipart eKYC request.
- Start MailHog at `http://localhost:8025`. Run the separate **Fetch ... OTP from MailHog** request
  after requesting an OTP. It reads the newest matching email and saves the six-digit code into `otp`.
- For password recovery, run **Forgot password**, then **Fetch password reset token from MailHog**,
  then **Reset password**. The collection saves `resetToken` automatically and updates
  `customerPassword` to the new password after a successful reset.
- Run **List my accounts**, **Set initial transaction PIN**, and **Validate transfer prerequisites**
  before requesting OTP. These requests save `sourceAccountId` and report inactive or insufficient-balance
  source accounts clearly.
- Enter `targetAccountId` manually for an internal transfer. It must be a different active account ID;
  it is not a customer/user ID and does not require an Admin API call.
- Ensure the source account has at least `transferAmount` before testing transfers.
- A newly registered customer uses `transactionPin=1234` as the initial PIN. `Change PIN` changes it
  to `newTransactionPin=5678`, then swaps both environment values after success so repeated tests remain valid.
