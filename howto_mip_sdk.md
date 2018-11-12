# HowTo verify MIP SDK access

## Create an Azure AD App Registration

Authentication against the Azure AD tenant requires creating a native application registration. The client ID created in this step is used in a later step to generate an OAuth2 token.

1. Go to https://portal.azure.com and log in as a global admin.
   > Your tenant may permit standard users to register applications. If you aren't a global admin, you can attempt these steps, but may need to work with a tenant administrator to have an application registered or be granted access to register applications.
2. Click Azure Active Directory, then **App Registrations** in the menu blade.
3. Click **View all applications**
4. Click **New Applications Registration**
5. For name, enter **MipSdk-Sample-Apps**
6. Set **Application Type** to **Native**
7. For Redirect URI, enter **mipsdk-auth-sample://authorize**
   > Note: This can be anything you'd like, but should be unique in the tenant.
8. Click **Create**

The **Registered app** blade should now be displayed.

Extract the `Application ID` from the apps overview page. You will need it later.

Now add MIP permissions to you application:

1. Click **Settings**
2. Click **Required Permissions**
3. Click **Add**
4. Click **Select an API**
5. Select **Microsoft Rights Management Services** and click **Select**
6. Under **Select Permissions** select **Create and access protected content for users**
7. Click **Select** then **Done**
8. Click **Add**
9. Click **Select an API**
10. In the search box, type **Microsoft Information Protection Sync Service** then select the service and click **Select**
11. Under **Select Permissions** select **Read all unified policies a user has access to.**
12. Click **Select** then **Done**
13. In the **Required Permissions** blade, click **Grant Permissions** and confirm.

Now generate a client secret for you app:

1. Click **Settings**
2. Click **Keys**
3. Fill a **Password**
4. Click **Save**
5. Copy the **Value** after save. You will need it later.

## Download and unpack MIP SDK

1. Go to https://aka.ms/MIPSDKBinaries and download the SDK for your respecive platform
2. Inside the download ZIP unpack the file_sdk zip.
3. Go to file folder for you HW platform, e.g. `mip_sdk_file_macos_1.0.49/bins/release/x86_64`.

## Run file sample in your ADD and MIP environment

1. First you have to get a token from AAD. Here you will need your AAD tenant/directory, e.g. `company.onmicrosoft.com` and application ID and client secret as noted above.

![POST for AAD access token][token.png]

2. The response Json document contains the OAuth2 `access_token`.
3. Run now `file_sample`, e.g. `./file_sample --username user_that_does_protection@contoso.com --rights READ,VIEW --protect user_that_can_read@contoso.com --file UnProtected.docx --clientid YOUR_AAD_APPLICATION_ID --protectiontoken YOUR_ACCESS_TOKEN`.

In this case the response should contain something like:

```bash
New file created: UnProtected_modified.docx
```

Now open the file either with office or in case of PDF or image file with [AIP viewer](https://www.microsoft.com/en-us/download/details.aspx?id=54536) as user `user_that_can_read@contoso.com`.
