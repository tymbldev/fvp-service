# Google Sheets API Configuration

## API Key Authentication

This application uses Google Sheets API with API key authentication for read-only access to Google Sheets.

### Configuration

The following properties are configured in the application properties files:

```properties
# Google Sheets Configuration
google.sheets.spreadsheet.id=113Kn4qHrOnqOd1s7FEDHLpUrrA3VXY-DSd9keKy9zSg
google.sheets.api.key=AIzaSyD6F7BgtnjapAu8fJlLa9AeuYps7YV7tSY
google.sheets.application.name=FVP-Service
```

### Security Considerations

- The API key is restricted to read-only access to Google Sheets.
- For additional security, consider setting up API key restrictions in the Google Cloud Console:
  - Restrict the API key to only the Google Sheets API
  - Set up HTTP referrer restrictions to limit which domains can use the key
  - Consider using IP address restrictions for production environments

### Updating the Configuration

If you need to use a different Google Sheet or API key:

1. Update the `google.sheets.spreadsheet.id` property with your Google Sheet ID
2. Update the `google.sheets.api.key` property with your Google API Key
3. Make sure the Google Sheet is publicly accessible or shared with the appropriate permissions 