# Android XVideos App - Flowchart

## Application Architecture Flow

```mermaid
graph TD
    A[App Start] --> B[SplashActivity]
    B --> C{Permissions Check}
    C -->|No Permissions| D[PermissionScreenActivity]
    C -->|Has Permissions| E[MainActivity]
    D --> F[Request Permissions]
    F --> G{Permissions Granted?}
    G -->|No| F
    G -->|Yes| E
    
    E --> H[ScreenRoot Navigation]
    H --> I[Menu Screen]
    
    I --> J[XVideos Dashboard]
    I --> K[Luscious Explorer]
    I --> L[RedGifs Explorer]
    
    %% XVideos Module Flow
    J --> J1[ScreenXDashBoards]
    J1 --> J2[DashboardsPaginatedListScreen]
    J1 --> J3[ScreenConfig]
    J1 --> J4[ScreenFavorites]
    J2 --> J5[Video Player]
    J3 --> J6[Settings Management]
    J4 --> J7[Favorites Management]
    
    %% Luscious Module Flow
    K --> K1[ScreenLExplorer]
    K1 --> K2[ScreenLAlbumTopHits]
    K1 --> K3[ScreenLConfigTab]
    K1 --> K4[ScreenSaved]
    
    K2 --> K5[ScreenAlbum]
    K2 --> K6[ScreenAlbumList]
    K4 --> K7[ScreenLSavedAlbumsTab]
    K4 --> K8[ScreenLSavedLikesTab]
    K4 --> K9[ScreenLSavedLCryptoTab]
    
    %% RedGifs Module Flow
    L --> L1[ScreenRedRoot]
    L1 --> L2[ScreenExplorer]
    L1 --> L3[ScreenRedProfile]
    L1 --> L4[ScreenNiche]
    L1 --> L5[ScreenRedTopThisWeek]
    L1 --> L6[ScreenRedManageBlock]
    
    L2 --> L7[ScreenSaved]
    L2 --> L8[ScreenRedFullScreen]
    
    %% Common Components
    H --> M[EventBus System]
    H --> N[Network Traffic Monitor]
    H --> O[Video Player Cache Manager]
    H --> P[Coil Image Loader]
    
    %% Database Layer
    M --> Q[Room Database]
    Q --> Q1[Cache Media Response DAO]
    Q --> Q2[Post Json Ram DAO]
    Q --> Q3[Collection DB]
    Q --> Q4[File DB]
    
    %% Network Layer
    P --> R[HTTP Client]
    R --> R1[KDownloader]
    R --> R2[Progress Manager]
    
    %% Security Layer
    O --> S[Crypto Manager]
    S --> S1[Encrypted File Model]
    S --> S2[Password Manager]
```

## Application Startup Sequence

```mermaid
sequenceDiagram
    participant App
    participant Splash
    participant Main
    participant DB
    participant Network
    participant UI
    
    App->>Splash: onCreate()
    Splash->>DB: Initialize Database
    Splash->>Network: Initialize Network Components
    Splash->>UI: Load Cached Data
    DB-->>Splash: Data Ready
    Network-->>Splash: Network Ready
    UI-->>Splash: UI Ready
    Splash->>Main: startActivity()
    Main->>UI: Set Content with ScreenRoot
    UI->>Main: Show Menu Screen
```

## Module Dependencies

```mermaid
graph LR
    subgraph "Core Modules"
        A[App.kt]
        B[ScreenRoot.kt]
        C[MainActivity.kt]
    end
    
    subgraph "Common Components"
        D[EventBus]
        E[Network Monitor]
        F[Image Loader]
        G[Video Cache]
        H[Crypto Manager]
    end
    
    subgraph "XVideos Module"
        I[ScreenXDashBoards]
        J[DashboardsPaginatedList]
        K[ScreenConfig]
        L[ScreenFavorites]
    end
    
    subgraph "Luscious Module"
        M[ScreenLExplorer]
        N[ScreenAlbum]
        O[ScreenSaved]
    end
    
    subgraph "RedGifs Module"
        P[ScreenRedRoot]
        Q[ScreenExplorer]
        R[ScreenProfile]
        S[ScreenNiche]
    end
    
    A --> D
    A --> E
    A --> F
    B --> D
    B --> E
    B --> F
    B --> G
    C --> H
    
    B --> I
    B --> M
    B --> P
    
    I --> D
    M --> D
    P --> D
```

## Data Flow Architecture

```mermaid
graph TD
    A[UI Layer] --> B[ViewModel/ScreenModel]
    B --> C[Repository Layer]
    C --> D[Data Sources]
    
    subgraph "Data Sources"
        D1[Remote API]
        D2[Local Database]
        D3[File System]
        D4[Encrypted Storage]
    end
    
    subgraph "Network Layer"
        E1[KDownloader]
        E2[HTTP Client]
        E3[Progress Manager]
    end
    
    subgraph "Database Layer"
        F1[Room Database]
        F2[DAOs]
        F3[Entities]
    end
    
    D --> D1
    D --> D2
    D --> D3
    D --> D4
    
    D1 --> E1
    D1 --> E2
    D1 --> E3
    
    D2 --> F1
    F1 --> F2
    F2 --> F3
```

## Permission Flow

```mermaid
stateDiagram-v2
    [*] --> CheckPermissions
    CheckPermissions --> HasPermissions: Permissions Granted
    CheckPermissions --> RequestPermissions: Permissions Missing
    
    RequestPermissions --> ShowPermissionScreen
    ShowPermissionScreen --> WaitingForUser
    WaitingForUser --> CheckPermissions: User Response
    
    HasPermissions --> InitializeApp
    InitializeApp --> [*]
    
    ShowPermissionScreen --> [*]: App Closed
```

## Key Components Overview

### 1. **Application Entry Points**
- `SplashActivity`: App initialization and data loading
- `MainActivity`: Main UI container with navigation
- `PermissionScreenActivity`: Permission management

### 2. **Navigation Structure**
- `ScreenRoot`: Root navigation container
- `MenuScreen`: Main menu with three module options
- Module-specific navigators for each content type

### 3. **Content Modules**
- **XVideos**: Video content from xvideos.com
- **Luscious**: Image content from luscious.net
- **RedGifs**: GIF content from redgifs.com

### 4. **Common Infrastructure**
- **EventBus**: Inter-component communication
- **Network Monitor**: Traffic statistics
- **Image Loading**: Coil-based image management
- **Video Caching**: ExoPlayer with cache management
- **Encryption**: File encryption/decryption
- **Database**: Room-based local storage

### 5. **Data Management**
- **Remote APIs**: HTTP-based content fetching
- **Local Cache**: Room database for offline access
- **File Storage**: Encrypted local file management
- **Download Manager**: Background file downloading

## Technology Stack

- **UI Framework**: Jetpack Compose
- **Navigation**: Voyager
- **Dependency Injection**: Hilt
- **Database**: Room
- **Image Loading**: Coil
- **Video Player**: ExoPlayer
- **Networking**: OkHttp + Custom HTTP Client
- **Encryption**: Custom Crypto Implementation
- **Architecture**: MVVM with ScreenModel pattern
