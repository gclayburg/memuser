# Changelog

## [0.9.0-SNAPSHOT]
SCIM 2.0 PATCH/filter/Groups

### Fixed
- better error messages when user cannot be created
- Json requests now use case-insensitive parsing

### Added
- /{domain}/Groups support per SCIM 2.0 spec (RFC 7643 RFC 7644) 
- /{domain}/ServiceProviderConfig and /{domain}/serviceConfiguration
- now using unboundid SCIM library
- now using Spring Boot 2.5.2
- upgraded many dependencies for compatibility with Spring Boot 2.5.2
- /{domain}/User now supports SCIM features like filter,sort,attributes,excludedAttributes
- support for SCIM PATCH /{domain}/Users/{id}
- support for SCIM PATCH /{domain}/Groups/{id}
- upgrade gradle wrapper to version 7.0.2
- optimized docker image layers
- upgrade upbanner-starter to 2.3.3
- support for SCIM GET /{domain}/ResourceTypes
- support for SCIM GET /{domain}/Schemas


## [0.8.1] - 2021-6-23
Bug fix release

### Changed
- upgraded upbanner-starter to 2.3.1 for better compatibility with spring-boot-devtools

### Fixed
- SCIM Uris were case sensitive.  They are case-insensitive now.
- SCIM attribute names were case sensitive.  They are case-insensitive now.
- "active" attribute on a User is now forced to be parsed as boolean and not an arbitrary type

## [0.8.0] - 2020-3-24
Multiple Domains

### Added
- new option to add unlimited number of test users on startup
- new ability for multiple domains of Users where each domain is stored completely independent from each other

### Notes
To use the multiple domain feature, just change the url. For example, use this for the fakehr, contractors and partners domains.
http://localhost:8080/api/multiv2/fakehr/Users/
http://localhost:8080/api/multiv2/contractors/Users/
http://localhost:8080/api/multiv2/partners/Users/

No setup is required. All data is still stored in-memory only.

## [0.7.3] 2020-3-9
Unreleased bug fixes

### Added
- initial ability to add test users on startup

### Fixed
- timestamps should be returned with millisecond precision and not truncated

## [0.7.2] 2020-2-18
Proxy integration

### Added
- requests now honor X-Forwarded-Proto and X-Forwarded-Host headers for use behind a proxy

## [0.7.0] 2020-1-17
Pageable Users

### Added
- GET /api/v2/User is now pageable using 2 different clients
    - responses are now pageable per SCIM RFC 7644.  pingidentity SCIM 2.0 client uses this page format
    - responses are now pageable per RFC 5988. jhipster generated clients use this page format
- added CORS support

## [0.6.5] 2019-3-4
initial SCIM 2.0 support

### Added
- option to load test users on startup
- ability to run under HTTPS

### Fixed
- better handling of ISO 8601 Dates
- correct handling of meta.location
- corrected /ServiceProviderConfig
- HTTP PUT /Users/{id} now works
- HTTP DELETE /Users/{id} now works
- HTTP DELETE /Users/ now works to delete all users without needing to restart
- fixed schemas parsing

## [0.6.0] 2017-10-18

## [0.5.9] 2017-9-17

## [0.5.7] 2017-9-1

## [0.5.0] 2017-8-25
