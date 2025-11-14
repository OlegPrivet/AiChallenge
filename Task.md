You are a senior Kotlin Multiplatform developer working on a KMP + Compose project that already contains a chat feature and a ChatMessage class:

- Package with chat messages: org.oleg.ai.challenge.component.chat.ChatMessage (and its child classes).
- UI is built with Compose (KMP/Compose Multiplatform).
- Persistence on Android must use Room.

Your task is to **add full chat persistence and a split-screen UI** according to the specification below, integrating into the existing architecture instead of rewriting it from scratch.

====================================
FEATURE DESCRIPTION (MUST IMPLEMENT)
====================================

1) ROOM DATABASE STRUCTURE
--------------------------

Add chat saving into the Room database with **three logical tables**:

A. Chat list table
------------------
Create an entity (e.g. `ChatEntity`) representing a chat:

- Columns:
    - `chatId`: primary key (String or Long – pick what matches existing code; if none exists, use Long with autoGenerate = true).
    - `chatName`: String — must be taken from the **first message** of the chat.
    - (Optional but recommended) `createdAt`, `updatedAt` timestamps.

Requirements:
- When a new chat is created, an empty chat entry can be inserted with a default name (e.g. "New chat").
- Once the first message for this chat is persisted, update `chatName` using that message’s content (see below for exact logic).
- This table is used solely for listing chats in the left pane.

B. Agents and subagents table
-----------------------------
Create an entity (e.g. `AgentEntity`) for agents and subagents related to chats.

Each row describes one agent or subagent used in a specific chat.

Fields (columns) to include:

- `id`: primary key for the Room entity (Long with autoGenerate = true, or a stable ID type that fits project).
- `agentId`: ID of the agent itself (can be String – whatever is currently used in the project for identifying agents).
- `chatId`: foreign key referencing the chat (from `ChatEntity.chatId`).
- `agentName`: human-readable name of the agent.
- `systemPrompt`: text for the system prompt.
- `assistantPrompt`: text for an assistant/instruction prompt if applicable.
- `model`: AI model used by this agent (String).
- `temperature`: Double/Float representing agent temperature.
- `isMain`: Boolean flag indicating whether this is the main chat agent.
- (Optional) `parentAgentId` if subagents need hierarchical linking (subagent -> main agent).

Requirements:
- The "main chat agent" is considered the primary agent for the chat.
- Subagents are stored in the same table; use `isMain` and/or `parentAgentId` to distinguish them.
- There must be a clear way to query all agents for a given chat (`chatId`).

C. Messages table
-----------------
Create a messages table (e.g. `MessageEntity`) that stores all chat messages.

Requirements:

1. Use the existing `org.oleg.ai.challenge.component.chat.ChatMessage` class and its child classes as the **source of the fields**.

    - Inspect `ChatMessage` and its subclasses (e.g. User message, Assistant message, System message, Tool message, etc.).
    - Design the `MessageEntity` so that:
        - Common fields across all message types are normal columns.
        - Each specialized “payload” is represented by an Embedded value object.

2. The entity should include at least:

   Columns:
    - `id`: primary key for the message (Long with autoGenerate = true).
    - `chatId`: foreign key to `ChatEntity.chatId`.
    - `agentId`: nullable foreign key to `AgentEntity.agentId` if the message is associated with a particular agent (e.g. assistant response).
    - `timestamp`: message creation time.
    - `role` or `type`: discriminator indicating which subclass of `ChatMessage` this row represents (user, assistant, system, tool, etc.).
    - `isFromUser`: Boolean or equivalent if that fits current model.
    - Any other fields that exist in `ChatMessage` base class (IDs, metadata, etc.)

   Embedded payloads:
    - For each child class of `ChatMessage`, create a corresponding data class that can be annotated with `@Embedded` inside `MessageEntity` (with a `prefix` to avoid column name conflicts).
    - All child classes must be represented via `@Embedded` in the message table.
    - Use a type discriminator (e.g. `messageType: String` or enum stored via @TypeConverter) to know which embedded payload is active.

3. Create needed `@TypeConverter`s for:
    - Enums (message type, role, etc.).
    - Complex types in `ChatMessage` and its subclasses (if arrays, lists, maps or sealed hierarchies need to be persisted).

4. Define proper `@ForeignKey` relationships:

    - `MessageEntity.chatId` -> `ChatEntity.chatId`.
    - `MessageEntity.agentId` -> `AgentEntity.agentId` (nullable).

5. DAO & Database:

    - Add a `ChatDao` that provides:
        - `getAllChats(): Flow<List<ChatEntity>>`
        - `insertChat(chat: ChatEntity): Long`
        - `updateChat(chat: ChatEntity)`
        - `deleteChat(chatId: ...)` (optional)

    - Add an `AgentDao` that provides:
        - `getAgentsForChat(chatId: ...): Flow<List<AgentEntity>>`
        - `insertAgents(agents: List<AgentEntity>)`
        - `updateAgent(agent: AgentEntity)`

    - Add a `MessageDao` that provides:
        - `getMessagesForChat(chatId: ...): Flow<List<MessageEntity>>`
        - `insertMessage(message: MessageEntity)`
        - `insertMessages(messages: List<MessageEntity>)`
        - (Optionally) `deleteMessagesForChat(chatId: ...)`.

    - Integrate these DAOs into the existing RoomDatabase subclass (or create one if not present yet), including any required migrations.

2) INTEGRATION WITH EXISTING CHAT LOGIC
---------------------------------------

Wire the Room layer into existing chat logic:

- Whenever a chat is created via the UI, create a `ChatEntity` in the database and return/use its `chatId`.
- Whenever a message is sent or received in the chat:
    - Convert from the in-memory `ChatMessage` model into `MessageEntity`.
    - Persist it via `MessageDao.insertMessage`.
- When the app starts or the chat screen is opened, load messages from Room using the given `chatId` and expose them via a Value into the Component.
- Ensure that the main agent and subagents for a chat are also loaded from `AgentEntity` and used by the chat logic.
- Make sure that chat name in `ChatEntity.chatName` is updated from the **first meaningful chat message**:
    - Use the content of the first user message (or first non-system message) as `chatName`.
    - Perform this update lazily when the first message is persisted.

3) UI: SPLIT MAIN SCREEN INTO TWO PANELS
----------------------------------------

Modify the main screen so that it displays **two screens side by side**:

Layout requirements:

- Use a `Row(modifier = Modifier.fillMaxSize())` (for desktop/tablet) or another responsive layout that:
    - On wide screens: left pane and right pane side by side.
    - On small screens: if necessary, you can fallback to a stacked layout, but prioritize side-by-side.

Left pane (chat list panel):
- At the top: a “Create chat” button.
- Below the button: a list of chats (LazyColumn) backed by the Room chat list (`ChatDao.getAllChats()`).
- Each list item displays:
    - The chat name (`chatName`).
    - Optional subtitle with last message preview or creation date (nice-to-have).
- When a chat item is tapped/clicked:
    - Select that chat (update selectedChatId in state).
    - The right pane then shows this selected chat.

Right pane (chat creation + chat screen panel):
- The right side should show:
    - A **chat creation area** (e.g. fields for selecting/creating agents, model, temperature, etc., if such UI already exists in the project).
    - The **chat screen itself** (messages list + input field).
- Behavior:
    - When no chat is selected yet:
        - Show the chat creation UI that allows creating a new chat.
    - When a chat is selected on the left:
        - Load chat agents and messages from Room and display:
            - Existing chat messages in a scrollable list (LazyColumn).
            - Input field to send new messages.
        - All new messages must be persisted to Room as they are sent/received.

State and Component:
- Introduce (or extend) a Component (or equivalent shared state holder) for the main screen which exposes:
    - `chatList: Value<List<ChatUiModel>>` backed by Room.
    - `selectedChatId: Value<Long?>` (or String, depending on `chatId` type).
    - `selectedChatMessages: Value<List<ChatMessageUiModel>>`.
    - `agentsForSelectedChat: Value<List<AgentUiModel>>`.
- Implement functions:
    - `createNewChat()`:
        - Inserts a new `ChatEntity` in Room.
        - Sets `selectedChatId` to the new chat.
    - `selectChat(chatId: ...)`:
        - Updates `selectedChatId`.
        - Triggers loading of messages and agents from Room.
    - `sendMessage(chatId: ..., messageContent: ...)`:
        - Creates an in-memory ChatMessage.
        - Persists as `MessageEntity`.
        - Updates UI state (live Flow from Room will handle it).

4) ARCHITECTURE & QUALITY REQUIREMENTS
--------------------------------------

- Do not break existing chat functionality; extend it.
- Reuse existing domain models where possible (e.g. ChatMessage) and create clear mapping functions between domain and persistence entities.
- Place Room entities, DAOs and database in appropriate `data`/`persistence` module or package consistent with the project structure.
- Write at least basic unit tests for mapping functions (ChatMessage <-> MessageEntity) and for DAOs (where possible, using in-memory Room).
- Ensure all code compiles and fits into existing KMP/Compose architecture.
- Follow best practices for:
    - Room (@Entity, @PrimaryKey, @Embedded, @TypeConverter, @ForeignKey, etc.).
    - Compose (stateless UI with state hoisted into Component).
    - Coroutines + Value for asynchronous data loading.
    - Decompose navigation

5) ACCEPTANCE CRITERIA
----------------------

The implementation is complete when:

1. The app maintains a **persistent chat list** in Room:
    - Newly created chats appear immediately on the left pane.
    - Chat names are based on the first message in each chat.

2. Agents and subagents for each chat are saved and reloaded correctly:
    - On selecting a chat, its main agent and subagents are correctly loaded and used.

3. Messages are fully persisted and restored:
    - After closing and reopening the app, selecting a chat shows its full previous message history.
    - Different `ChatMessage` subclasses are correctly reconstructed from `MessageEntity` and its embedded fields.

4. Main screen split UI works:
    - Left: create chat button + chat list.
    - Right: chat creation UI + chat screen for the selected chat.
    - Clicking on a chat on the left opens that chat on the right, including its history and agents.

5. All code builds successfully and passes existing tests plus any new tests you add for this feature.
