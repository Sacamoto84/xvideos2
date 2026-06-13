# Навигация (Voyager)

Карта навигации приложения: корневой `Navigator`, секции X / L / R, табы
L-раздела, вложенный навигатор коллекции и fullscreen-оверлей.

- **Оранжевым** — вложенный `Navigator(ScreenCollectionName)` внутри
  collection-таба (открытая коллекция остаётся в области таба, нижние навбары
  видны).
- **Зелёным** — `L_FullScreenImage`: глобальный оверлей, пушится на
  **корневой** навигатор (`LocalMainNavigator`), поэтому кроет весь экран из
  любого вложенного контекста.

```mermaid
flowchart LR
  Root["ScreenRoot — корневой Navigator"]
  Menu["MenuScreen — главное меню"]
  Settings["AppSettingsScreen"]
  P2pRecv["ScreenP2pReceive"]
  X["ScreenXDashBoards — секция X"]
  R["R_Screen_Root — секция R"]
  LExp["L_ScreenExplorer — L, нижние табы"]

  Root --> Menu
  Menu -->|"push"| Settings
  Menu -->|"push"| P2pRecv
  Menu -->|"push"| X
  Menu -->|"push"| R
  Menu -->|"push"| LExp

  subgraph LTabs["L_ScreenExplorer — табы (свап контента, общий навигатор)"]
    AlbumList["L_ScreenAlbumList"]
    Saved["L_SavedTab"]
    TopHits["L_ScreenAlbumTopHits"]
    Search["L_ScreenAlbumSearch"]
  end
  LExp --> AlbumList
  LExp --> Saved
  LExp --> TopHits
  LExp --> Search

  subgraph SavedSub["L_SavedTab — подтабы"]
    Likes["Likes"]
    Coll["Collection"]
    AlbumsTab["Saved Albums"]
  end
  Saved --> Likes
  Saved --> Coll
  Saved --> AlbumsTab

  Album["ScreenLAlbum"]
  Tag["ScreenLAlbumLandingTag"]
  Filtered["L_ScreenAlbumList — фильтр"]
  P2pSend["ScreenP2pSend"]
  CollNav["Navigator вложенный"]
  CollName["ScreenCollectionName"]
  Full["L_FullScreenImage — оверлей"]

  AlbumList -->|"push"| Album
  TopHits -->|"push"| Album
  Search -->|"push"| Album
  AlbumsTab -->|"push"| Album
  Album -->|"push"| Tag
  Album -->|"push"| Filtered
  Album -->|"push"| P2pSend
  Tag -->|"push"| Album

  Coll -->|"вложенный Navigator, остаётся в табе"| CollNav
  CollNav --> CollName
  Coll -->|"push"| P2pSend

  Likes -->|"открыть фото — push на корневой Navigator"| Full
  Album -->|"push на корневой Navigator"| Full
  CollName -->|"push на корневой Navigator"| Full
  Full -->|"push"| Album

  classDef nested fill:#FFE0B2,stroke:#E65100,color:#000
  classDef overlay fill:#C8E6C9,stroke:#2E7D32,color:#000
  class CollNav,CollName nested
  class Full overlay
```

## Заметки

- L-табы (`AlbumList / SavedTab / TopHits / Search`) — это свап контента
  (`when(screenType) { ... .Content() }`) в **одном** навигаторе, не вложенные
  навигаторы. Поэтому push из них (например `ScreenLAlbum`) кроет весь экран.
- Единственный вложенный навигатор в L-потоке — `Navigator(ScreenCollectionName)`
  в collection-табе. Любой `push` из него (включая fullscreen, до фикса)
  замыкается в области таба — поэтому fullscreen роутится на корневой навигатор
  через `LocalMainNavigator`.
- FigJam-версия (онлайн, редактируемая):
  https://www.figma.com/board/QVsa8oJOkWrFfdCP7FGNi1
