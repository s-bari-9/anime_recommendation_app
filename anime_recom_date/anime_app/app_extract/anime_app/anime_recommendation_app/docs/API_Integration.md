# MyAnimeList API Integration

This document provides a comprehensive explanation of the MyAnimeList API integration in the AnimeRec application.

## Components Overview

The API integration consists of several key components:

1. **MyAnimeListService**: Retrofit interface defining all the API endpoints
2. **MyAnimeListClient**: Client class that handles authentication, caching, and rate limiting
3. **ApiResponseCache**: Caching layer to minimize network requests
4. **RetrofitExtensions**: Utilities for handling API rate limits and retries
5. **AnimeRepositoryImpl**: Repository implementation that uses the API client
6. **AuthManager**: Handles authentication tokens and user session management

## Authentication Flow

The application uses OAuth 2.0 with PKCE (Proof Key for Code Exchange) as required by the MyAnimeList API:

1. User initiates login
2. App generates a code verifier and challenge using the `OAuthUtil`
3. App opens the browser with MyAnimeList authorization URL
4. User logs in to MyAnimeList and grants permissions
5. MyAnimeList redirects back to the app with an authorization code
6. App exchanges the code for access and refresh tokens
7. Tokens are securely stored using `SecureStorage`

## API Client Features

### Caching Mechanism

The application implements a multi-level caching strategy:

- **In-memory cache**: Stores API responses to minimize network requests
- **Cache expiration**: Different expiration times for different types of data:
  - Short-lived (10 minutes): Dynamic data like user recommendations
  - Medium-lived (1 hour): Semi-static data like user lists
  - Long-lived (4 hours): More stable data like anime details
  - Very long-lived (24 hours): Static data like seasonal anime

### Rate Limiting

The API client handles MyAnimeList's rate limits:

1. **Detection**: Identifies 429 (Too Many Requests) responses
2. **Retry-After Header**: Respects the server's requested wait time
3. **Exponential Backoff**: Implements exponential backoff for retries
4. **Request Spacing**: Adds short delays between paginated requests

### Error Handling

The application implements comprehensive error handling:

1. **Network Errors**: Retries on temporary failures
2. **API Errors**: Parses and presents user-friendly error messages
3. **Authentication Errors**: Automatically refreshes tokens or prompts for re-login
4. **Resource Class**: Uses a `Resource<T>` wrapper to handle success/error states

### Pagination

For API endpoints that return paginated results:

1. **Automatic Pagination**: Handles fetching multiple pages transparently
2. **Result Combination**: Combines results from multiple pages
3. **Efficient Loading**: Loads only what's needed with sensible limits

## API Usage Guidelines

### Available Endpoints

The integration covers all essential MyAnimeList endpoints:

- **User Profile**: Get current user information
- **User Lists**: Get anime/manga lists with filtering
- **Content Details**: Get detailed information about specific anime/manga
- **Recommendations**: Get personalized recommendations
- **Updates**: Update anime/manga status in user lists
- **Rankings**: Get top anime/manga by various criteria
- **Seasonal**: Get seasonal anime listings

### Fields Optimization

To optimize network usage, the application:

1. Specifies exactly which fields to fetch for each request
2. Uses predefined field sets for different content types
3. Avoids requesting unnecessary data

### Batch Processing

For operations like updating multiple items:

1. Processes updates in batches to avoid rate limits
2. Uses coroutines for concurrent operations where appropriate
3. Implements proper error handling for batch operations

## Data Models

The API responses are mapped to application-specific models:

- **AnimeContent**: Unified model for anime, manga, and novels
- **User**: User profile with preferences
- **AuthTokens**: Authentication token storage

## Monitoring and Debugging

For debugging API issues:

1. **Logging**: Comprehensive logging of API requests and responses
2. **HTTP Interceptors**: OkHttp interceptors to monitor network traffic
3. **Cache Debugging**: Methods to inspect and clear caches

## Common Issues and Solutions

### Rate Limiting

If you encounter rate limiting:

1. Implement proper retry logic with exponential backoff
2. Add delays between requests in pagination
3. Optimize caching to reduce unnecessary requests

### Authentication Errors

If you encounter authentication issues:

1. Check token expiration and refresh logic
2. Verify client ID and redirect URI configurations
3. Test the PKCE implementation

### Network Performance

To improve network performance:

1. Use the cache effectively
2. Request only the fields you need
3. Implement proper pagination
4. Consider background synchronization for large datasets