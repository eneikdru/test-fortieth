<script>
  // Active Tab: 'search' (Knowledge Base), 'lms' (Canvas LMS Sync), 'messenger' (Messenger Subscriptions), 'profile' (Profile & Admin Tools)
  let currentTab = 'search';

  // --- MOODLE FINANCIAL CONFIGURATION STATE ---
  let moodleConfigScript = JSON.stringify({
    "course": "Financial Administration 2026",
    "categories": [
      {
        "name": "Basics of Wealth",
        "description": "Fundamental principles of budgeting and interest rates",
        "items": [
          {
            "title": "Personal Budgeting Masterclass",
            "type": "Book",
            "progress": "40%"
          },
          {
            "title": "Understanding Interest Rates",
            "type": "Page",
            "status": "Completed"
          }
        ]
      },
      {
        "name": "Advanced Growth",
        "description": "Intermediate level investing, risks, and market dynamics",
        "items": [
          {
            "title": "Stock Market 101",
            "type": "Book",
            "status": "New"
          },
          {
            "title": "Risk Assessment",
            "type": "Page",
            "status": "Prerequisite needed"
          }
        ]
      }
    ],
    "workloads": [
      {
        "specialty": "Epidemiology",
        "hours": 120,
        "clinicalHours": 80,
        "salaryRate": "1,800 RUB/hr",
        "guidelines": "Fulfill residency teaching requirements according to FBUN 2026 protocols."
      },
      {
        "specialty": "Pediatrics",
        "hours": 90,
        "clinicalHours": 60,
        "salaryRate": "1,650 RUB/hr",
        "guidelines": "Must align clinical pediatric schedules with Rospotrebnadzor standards."
      },
      {
        "specialty": "Infectious Diseases",
        "hours": 110,
        "clinicalHours": 70,
        "salaryRate": "1,750 RUB/hr",
        "guidelines": "Align with the state postgraduate standards and clinical safety procedures."
      }
    ]
  }, null, 2);

  let isConfiguring = false;
  let configureProgress = 0;
  let configureLogs = [];
  let isScaffolded = false;
  let moodleParsedConfig = null;

  function executeMoodleScript() {
    if (isConfiguring) return;
    isConfiguring = true;
    configureProgress = 0;
    configureLogs = ["[INFO] - Starting Moodle Dashboard Configuration Agent"];

    try {
      moodleParsedConfig = JSON.parse(moodleConfigScript);
    } catch (err) {
      configureLogs = [
        ...configureLogs,
        `[ERROR] - Failed to parse configuration script: ${err.message}`,
        "[ERROR] - Scaffolding aborted due to script errors."
      ];
      isConfiguring = false;
      return;
    }

    let steps = [
      { p: 15, log: "[INFO] - Successfully validated JSON structure and schemas." },
      { p: 35, log: `[INFO] - Provisioning Moodle Course: "${moodleParsedConfig.course || 'Financial Administration'}"` },
      { p: 55, log: `[INFO] - Creating categories: ${moodleParsedConfig.categories ? moodleParsedConfig.categories.map(c => '"' + c.name + '"').join(', ') : 'None'}` },
      { p: 75, log: "[INFO] - Generating Books (mod_book) and Pages (mod_page) for budget block." },
      { p: 90, log: "[INFO] - Deploying instructor workload framework guides and spreadsheets." },
      { p: 100, log: "[SUCCESS] - Moodle Financial Block structures automated and scaffolded successfully!" }
    ];

    let currentStepIdx = 0;
    let interval = setInterval(() => {
      if (currentStepIdx < steps.length) {
        let step = steps[currentStepIdx];
        configureProgress = step.p;
        configureLogs = [...configureLogs, step.log];
        currentStepIdx++;
      } else {
        clearInterval(interval);
        isConfiguring = false;
        isScaffolded = true;
      }
    }, 400);
  }

  function resetMoodleDashboard() {
    isScaffolded = false;
    configureLogs = ["[INFO] - Reset complete. Dashboard is empty. Run the script to scaffold."];
    configureProgress = 0;
  }

  // Active Role for demonstration
  let currentRole = 'ADMINISTRATOR'; // ADMINISTRATOR, CONTENT_MANAGER, TEACHER, LEARNER

  // --- KNOWLEDGE BASE DATA ---
  let initialDocuments = [
    {
      id: 1,
      title: "Strategic Plan 2024.pdf",
      description: "Detailed roadmap for international expansion and R&D allocation within the institute.",
      category: "нормативные акты",
      tags: ["ФБУН", "нормативные акты", "ординатура"],
      specialty: "Эпидемиология",
      educationLevel: "Ординатура",
      fileType: "PDF",
      fileSize: "12.8 MB",
      updatedAt: "2026-10-24 14:22:11",
      author: "Admin",
      versionNumber: 2,
      isFavorite: false,
      isSubscribed: false,
      history: [
        { version: 2, author: "Admin", date: "2026-10-24 14:22:11", desc: "Updated references to include new guidelines." },
        { version: 1, author: "System", date: "2026-03-12 09:00:00", desc: "Initial document upload." }
      ],
      comments: [
        { id: 1, author: "Dr. Smirnov", date: "2026-10-25 11:30", text: "Excellent resource. Fully aligned with current protocols." }
      ]
    },
    {
      id: 2,
      title: "Client Onboarding Specs.docx",
      description: "Technical requirements and security protocols for new tier-1 educational partners.",
      category: "шаблоны",
      tags: ["шаблоны", "аспирантура"],
      specialty: "Инфекционные болезни",
      educationLevel: "Аспирантура",
      fileType: "DOCX",
      fileSize: "450 KB",
      updatedAt: "2026-10-22 11:15:00",
      author: "Content Manager",
      versionNumber: 1,
      isFavorite: true,
      isSubscribed: false,
      history: [
        { version: 1, author: "Content Manager", date: "2026-10-22 11:15:00", desc: "First release of partner specs." }
      ],
      comments: []
    },
    {
      id: 3,
      title: "Marketing Budget.xlsx",
      description: "Quarterly spend breakdown across social, PPC, and event channels for regional departments.",
      category: "шаблоны",
      tags: ["шаблоны", "ДПО"],
      specialty: "Педиатрия",
      educationLevel: "ДПО",
      fileType: "XLSX",
      fileSize: "1.2 MB",
      updatedAt: "2026-10-20 18:40:02",
      author: "Finance Dept",
      versionNumber: 3,
      isFavorite: false,
      isSubscribed: false,
      history: [
        { version: 3, author: "Finance Dept", date: "2026-10-20 18:40:02", desc: "Finalized marketing figures." },
        { version: 2, author: "Finance Assistant", date: "2026-10-15 12:00:00", desc: "Draft with tentative estimates." },
        { version: 1, author: "Finance Dept", date: "2026-10-01 10:00:00", desc: "Template created." }
      ],
      comments: [
        { id: 1, author: "Director", date: "2026-10-21 09:12", text: "Approved. Make sure to track against Q4 goals." }
      ]
    },
    {
      id: 4,
      title: "Вопросы к ГИА по Эпидемиологии.pdf",
      description: "Официальный перечень вопросов для подготовки к государственной итоговой аттестации.",
      category: "вопросы к экзаменам",
      tags: ["ГИА", "вопросы к экзаменам", "ФБУН", "ординатура"],
      specialty: "Эпидемиология",
      educationLevel: "Ординатура",
      fileType: "PDF",
      fileSize: "3.1 MB",
      updatedAt: "2026-11-02 10:20:15",
      author: "Prof. Ivanova",
      versionNumber: 1,
      isFavorite: false,
      isSubscribed: true,
      history: [
        { version: 1, author: "Prof. Ivanova", date: "2026-11-02 10:20:15", desc: "Добавлены вопросы на 2026/2027 учебный год." }
      ],
      comments: [
        { id: 1, author: "Resident Petrov", date: "2026-11-03 14:02", text: "Спасибо за актуальные вопросы!" }
      ]
    },
    {
      id: 5,
      title: "ФГОС Ординатура Эпидемиология.pdf",
      description: "Федеральный государственный образовательный стандарт высшего образования по специальности Эпидемиология.",
      category: "нормативные акты",
      tags: ["ФГОС", "нормативные акты", "ординатура"],
      specialty: "Эпидемиология",
      educationLevel: "Ординатура",
      fileType: "PDF",
      fileSize: "1.8 MB",
      updatedAt: "2026-09-15 16:30:00",
      author: "Admin",
      versionNumber: 1,
      isFavorite: false,
      isSubscribed: false,
      history: [
        { version: 1, author: "Admin", date: "2026-09-15 16:30:00", desc: "Утвержденный стандарт." }
      ],
      comments: []
    },
    {
      id: 6,
      title: "Шаблон протокола ГЭК.docx",
      description: "Образец протокола заседания Государственной Экзаменационной Комиссии.",
      category: "шаблоны",
      tags: ["ГЭК", "шаблоны"],
      specialty: "Инфекционные болезни",
      educationLevel: "Ординатура",
      fileType: "DOCX",
      fileSize: "780 KB",
      updatedAt: "2026-08-11 11:12:04",
      author: "Content Manager",
      versionNumber: 2,
      isFavorite: false,
      isSubscribed: false,
      history: [
        { version: 2, author: "Content Manager", date: "2026-08-11 11:12:04", desc: "Обновлено форматирование шапки." },
        { version: 1, author: "System", date: "2026-05-04 10:00:00", desc: "Начальный шаблон протокола." }
      ],
      comments: []
    }
  ];

  let documents = [...initialDocuments];
  let recentlyViewedIds = [1, 2, 4];
  let savedSearches = ["ФГОС Эпидемиология", "Шаблоны ГЭК"];

  // --- KNOWLEDGE BASE INTERACTIVE STATE ---
  let searchQuery = '';
  let showSuggestions = false;
  let activeSuggestionIndex = -1;

  // Filter values
  let showFilterModal = false;
  let selectedDocType = 'ALL'; // ALL, PDF, DOCX, XLSX, PPTX, JPG/PNG
  let selectedSpecialty = 'ALL'; // ALL, Эпидемиология, Инфекционные болезни, Педиатрия
  let selectedLevel = 'ALL'; // ALL, Ординатура, Аспирантура, ДПО
  let sortBy = 'relevance'; // relevance, date, title

  // Suggestions with Synonym/Abbreviation mapping
  const abbrevSynonyms = {
    "ФБУН": "ФБУН ЦНИИ Эпидемиологии Роспотребнадзора",
    "ГЭК": "Государственная Экзаменационная Комиссия",
    "ГИА": "Государственная Итоговая Аттестация",
    "ФГОС": "Федеральный Государственный Образовательный Стандарт"
  };

  // Compute suggestions based on typing
  $: suggestions = (() => {
    if (!searchQuery.trim()) return [];
    const normalizedQuery = searchQuery.toLowerCase().trim();

    // Collect base matches from document titles and tags
    let rawSuggestions = [];

    // Check for abbreviation synonym matches
    Object.entries(abbrevSynonyms).forEach(([abbrev, fullName]) => {
      if (abbrev.toLowerCase().includes(normalizedQuery) || fullName.toLowerCase().includes(normalizedQuery)) {
        rawSuggestions.push({
          type: 'synonym',
          text: `${abbrev} (${fullName})`,
          searchVal: abbrev
        });
      }
    });

    // Match document titles, categories or tags
    documents.forEach(doc => {
      const matchTitle = doc.title.toLowerCase().includes(normalizedQuery);
      const matchTag = doc.tags.some(t => t.toLowerCase().includes(normalizedQuery));
      const matchDesc = doc.description.toLowerCase().includes(normalizedQuery);

      if (matchTitle || matchTag || matchDesc) {
        rawSuggestions.push({
          type: 'document',
          text: doc.title,
          searchVal: doc.title,
          id: doc.id
        });
      }
    });

    // Remove duplicates
    const seen = new Set();
    return rawSuggestions.filter(item => {
      const key = item.type + '|' + item.searchVal;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    }).slice(0, 5);
  })();

  // Filtered documents
  $: filteredDocuments = (() => {
    let result = [...documents];

    // Search query filter (with basic abbreviation synonym resolving)
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase().trim();

      result = result.filter(doc => {
        // Direct title, description or tag match
        const matchesDirect = doc.title.toLowerCase().includes(query) ||
                              doc.description.toLowerCase().includes(query) ||
                              doc.tags.some(tag => tag.toLowerCase().includes(query));

        if (matchesDirect) return true;

        // Synonym expansion logic (e.g. searching "Государственная Экзаменационная Комиссия" matches "ГЭК")
        for (const [abbrev, fullName] of Object.entries(abbrevSynonyms)) {
          const lowerAbbrev = abbrev.toLowerCase();
          const lowerFullName = fullName.toLowerCase();

          if (query.includes(lowerAbbrev) || query.includes(lowerFullName)) {
            const hasTag = doc.tags.some(t => t.toLowerCase() === lowerAbbrev || t.toLowerCase() === lowerFullName);
            const matchesText = doc.title.toLowerCase().includes(lowerAbbrev) || doc.description.toLowerCase().includes(lowerAbbrev);
            if (hasTag || matchesText) return true;
          }
        }

        return false;
      });
    }

    // Type filter
    if (selectedDocType !== 'ALL') {
      result = result.filter(doc => doc.fileType.toUpperCase() === selectedDocType.toUpperCase());
    }

    // Specialty filter
    if (selectedSpecialty !== 'ALL') {
      result = result.filter(doc => doc.specialty === selectedSpecialty);
    }

    // Education Level filter
    if (selectedLevel !== 'ALL') {
      result = result.filter(doc => doc.educationLevel === selectedLevel);
    }

    // Sorting
    if (sortBy === 'date') {
      result.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
    } else if (sortBy === 'title') {
      result.sort((a, b) => a.title.localeCompare(b.title));
    }

    return result;
  })();

  // --- DOCUMENT DETAILS VIEW DIALOG ---
  let selectedDocument = null;
  let showDetailModal = false;
  let newCommentText = '';
  let showUpdateForm = false;
  let updateRequestDesc = '';
  let exportProgress = 0;
  let isExporting = false;
  let exportType = ''; // PDF, DOCX
  let exportSuccessToast = '';

  function openDocumentDetails(doc) {
    selectedDocument = doc;
    showDetailModal = true;
    showUpdateForm = false;
    updateRequestDesc = '';
    // Add to recently viewed if not already at the front
    recentlyViewedIds = [doc.id, ...recentlyViewedIds.filter(id => id !== doc.id)].slice(0, 5);

    // Track view action in logs
    logUserAction("VIEW_DOCUMENT", `Viewed document ID: ${doc.id} (${doc.title})`);
  }

  function handleCommentSubmit() {
    if (!newCommentText.trim()) return;

    const updatedDoc = { ...selectedDocument };
    const newComment = {
      id: Date.now(),
      author: currentRole === 'ADMINISTRATOR' ? 'Administrator' : currentRole === 'CONTENT_MANAGER' ? 'Content Manager' : currentRole === 'TEACHER' ? 'Teacher / Supervisor' : 'Resident Student',
      date: new Date().toISOString().replace('T', ' ').substring(0, 16),
      text: newCommentText.trim()
    };

    updatedDoc.comments = [...updatedDoc.comments, newComment];
    documents = documents.map(d => d.id === updatedDoc.id ? updatedDoc : d);
    selectedDocument = updatedDoc;
    newCommentText = '';

    logUserAction("ADD_COMMENT", `Added comment on document ID: ${updatedDoc.id}`);
  }

  function submitUpdateRequest() {
    if (!updateRequestDesc.trim()) return;

    // Simulate update request submission
    showUpdateForm = false;
    updateRequestDesc = '';
    alert("Запрос на актуализацию документа успешно отправлен контент-менеджеру!");

    logUserAction("UPDATE_REQUEST", `Requested update for document ID: ${selectedDocument.id}`);
  }

  function simulateExport(type) {
    if (isExporting) return;
    isExporting = true;
    exportType = type;
    exportProgress = 0;

    let interval = setInterval(() => {
      exportProgress += 15;
      if (exportProgress >= 100) {
        clearInterval(interval);
        isExporting = false;
        exportSuccessToast = `Successfully exported and downloaded "${selectedDocument.title}" in ${type} format!`;
        setTimeout(() => {
          exportSuccessToast = '';
        }, 3500);

        logUserAction("EXPORT_DOCUMENT", `Exported document ID: ${selectedDocument.id} as ${type}`);
      }
    }, 150);
  }

  function toggleFavoriteInDetails() {
    const updated = { ...selectedDocument, isFavorite: !selectedDocument.isFavorite };
    documents = documents.map(d => d.id === updated.id ? updated : d);
    selectedDocument = updated;

    logUserAction("TOGGLE_FAVORITE", `${updated.isFavorite ? 'Favorited' : 'Unfavorited'} document ID: ${updated.id}`);
  }

  function toggleSubscribeInDetails() {
    const updated = { ...selectedDocument, isSubscribed: !selectedDocument.isSubscribed };
    documents = documents.map(d => d.id === updated.id ? updated : d);
    selectedDocument = updated;

    alert(updated.isSubscribed ? "Вы успешно подписались на уведомления об обновлениях этого документа!" : "Вы отписались от уведомлений.");
    logUserAction("TOGGLE_SUBSCRIBE", `${updated.isSubscribed ? 'Subscribed' : 'Unsubscribed'} updates for document ID: ${updated.id}`);
  }

  // --- UPLOAD MODAL (FAB) STATE ---
  let showUploadModal = false;
  let newDocTitle = '';
  let newDocDesc = '';
  let newDocCategory = 'нормативные акты';
  let newDocSpecialty = 'Эпидемиология';
  let newDocLevel = 'Ординатура';
  let newDocType = 'PDF';
  let newDocTags = '';

  function handleUploadSubmit() {
    if (!newDocTitle.trim() || !newDocDesc.trim()) {
      alert("Please fill in Title and Description.");
      return;
    }

    const tagsArr = newDocTags.split(',').map(t => t.trim()).filter(t => t.length > 0);
    tagsArr.push(newDocCategory);

    const newDoc = {
      id: Date.now(),
      title: newDocTitle.endsWith(`.${newDocType.toLowerCase()}`) ? newDocTitle : `${newDocTitle}.${newDocType.toLowerCase()}`,
      description: newDocDesc.trim(),
      category: newDocCategory,
      tags: tagsArr,
      specialty: newDocSpecialty,
      educationLevel: newDocLevel,
      fileType: newDocType,
      fileSize: "1.5 MB",
      updatedAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
      author: currentRole === 'ADMINISTRATOR' ? 'Administrator' : 'Content Manager',
      versionNumber: 1,
      isFavorite: false,
      isSubscribed: false,
      history: [
        { version: 1, author: currentRole, date: new Date().toISOString().substring(0, 10), desc: "Initial document upload." }
      ],
      comments: []
    };

    documents = [newDoc, ...documents];
    showUploadModal = false;

    // Reset Form
    newDocTitle = '';
    newDocDesc = '';
    newDocTags = '';

    alert("Документ успешно добавлен в базу знаний!");
    logUserAction("UPLOAD_DOCUMENT", `Uploaded document: ${newDoc.title}`);
  }

  function deleteDocument(id) {
    if (confirm("Вы действительно хотите удалить этот документ?")) {
      documents = documents.filter(d => d.id !== id);
      if (selectedDocument && selectedDocument.id === id) {
        showDetailModal = false;
      }
      logUserAction("DELETE_DOCUMENT", `Deleted document ID: ${id}`);
    }
  }

  // --- SYSTEM LOGS & ACTIONS AUDIT ---
  let userActionLogs = [
    { id: 1, user: "ADMINISTRATOR", action: "SYSTEM_INITIALIZE", details: "Knowledge base initial load complete", time: "2026-10-31 09:00:00" },
    { id: 2, user: "CONTENT_MANAGER", action: "UPLOAD_DOCUMENT", details: "Uploaded 'Strategic Plan 2024.pdf'", time: "2026-10-24 14:22:11" }
  ];

  function logUserAction(action, details) {
    const newLog = {
      id: Date.now(),
      user: currentRole,
      action: action,
      details: details,
      time: new Date().toISOString().replace('T', ' ').substring(0, 19)
    };
    userActionLogs = [newLog, ...userActionLogs];
  }

  // --- PREVIOUS CANVAS LMS SYNC STATE (PRESERVED) ---
  let syncStatus = 'failed'; // 'success', 'failed', 'syncing'
  let lastLmsSync = '2026-07-31 09:42:11';
  let lastEiosSync = '2026-07-31 09:30:15';
  let syncErrorMessage = 'Socket Connection Timeout: Failed to reach LMS endpoint within 10,000ms. Err: ETIMEDOUT';
  let syncProgress = 65;
  let syncedRecords = 4281;
  let avgLatency = '1.2s';
  let sourceEndpoint = 'canvas.instructure.com/api/v1';
  let authStatus = 'Valid OAuth2';
  let connectionProfile = 'REST API v2.0';

  // Mock activity logs
  let activityLogs = [
    {
      id: 1,
      type: 'success',
      title: 'Full Batch Sync Complete',
      time: '09:42:11',
      description: 'Processed 2,104 course enrollments and updated 42 user profiles.',
      tags: ['Status: 200 OK', 'Size: 4.2MB']
    },
    {
      id: 2,
      type: 'warning',
      title: 'Partial Record Mismatch',
      time: '08:00:04',
      description: '4 student IDs not found in local SIS. Skipping entries for next cycle.',
      tags: []
    }
  ];

  function startSync() {
    if (syncStatus === 'syncing') return;
    syncStatus = 'syncing';
    syncProgress = 0;

    let interval = setInterval(() => {
      if (syncProgress >= 100) {
        clearInterval(interval);
        syncStatus = 'success';
        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const formattedTime = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
        const formattedDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${formattedTime}`;
        lastLmsSync = formattedDate;
        lastEiosSync = formattedDate;

        activityLogs = [
          {
            id: Date.now(),
            type: 'success',
            title: 'Manual Batch Sync Complete',
            time: formattedTime,
            description: 'Successfully connected and verified metadata consistency with the main node.',
            tags: ['Status: 200 OK', 'Size: 1.8MB']
          },
          ...activityLogs
        ];
      } else {
        syncProgress += 20;
      }
    }, 150);
  }

  // --- PREVIOUS MESSENGER STATE (PRESERVED) ---
  let messengerSubscriptions = [
    {
      id: 1,
      userId: 'ADMINISTRATOR',
      channelOrChatId: '@eios_alerts_channel',
      notificationType: 'CRITICAL_ALERTS',
      isActive: true,
      createdAt: '2026-07-30 23:11:12'
    },
    {
      id: 2,
      userId: 'CONTENT_MANAGER',
      channelOrChatId: 'chat_99182',
      notificationType: 'SYNC_SUMMARY',
      isActive: false,
      createdAt: '2026-07-30 23:12:04'
    }
  ];

  let newUserId = 'ADMINISTRATOR';
  let newChannelId = '';
  let newNotificationType = 'CRITICAL_ALERTS';
  let showAddForm = false;

  function toggleSubscription(id) {
    messengerSubscriptions = messengerSubscriptions.map(sub => {
      if (sub.id === id) {
        return { ...sub, isActive: !sub.isActive };
      }
      return sub;
    });
  }

  function deleteSubscription(id) {
    messengerSubscriptions = messengerSubscriptions.filter(sub => sub.id !== id);
  }

  function addSubscription() {
    if (!newChannelId.trim()) return;
    const now = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const formattedDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

    const newSub = {
      id: Date.now(),
      userId: newUserId,
      channelOrChatId: newChannelId.trim(),
      notificationType: newNotificationType,
      isActive: true,
      createdAt: formattedDate
    };

    messengerSubscriptions = [...messengerSubscriptions, newSub];
    newChannelId = '';
    showAddForm = false;
  }

  // --- BACKUP & EXPORTS SYSTEM (ADMIN) ---
  let isBackingUp = false;
  let backupStatus = '';
  function triggerBackup() {
    isBackingUp = true;
    backupStatus = 'In Progress...';
    setTimeout(() => {
      isBackingUp = false;
      backupStatus = 'Successfully completed on ' + new Date().toISOString().replace('T', ' ').substring(0, 19);
      logUserAction("TRIGGER_BACKUP", "Initiated full system and database backup.");
    }, 1500);
  }

  // --- TEACHER COLLECTIONS SYSTEM ---
  let teacherCollections = [
    { name: "Осенний семестр - Эпидемиология", docs: [1, 4, 5] },
    { name: "Важно для подготовки к экзамену", docs: [4, 6] }
  ];
  let newCollectionName = '';
  function addCollection() {
    if (!newCollectionName.trim()) return;
    teacherCollections = [...teacherCollections, { name: newCollectionName.trim(), docs: [] }];
    newCollectionName = '';
    alert("Новая подборка успешно создана!");
  }

  // Keyboard navigation for Search Auto-Suggestions
  function handleSearchKeyDown(e) {
    if (!suggestions.length) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      activeSuggestionIndex = (activeSuggestionIndex + 1) % suggestions.length;
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      activeSuggestionIndex = (activeSuggestionIndex - 1 + suggestions.length) % suggestions.length;
    } else if (e.key === 'Enter') {
      if (activeSuggestionIndex >= 0 && activeSuggestionIndex < suggestions.length) {
        e.preventDefault();
        selectSuggestion(suggestions[activeSuggestionIndex]);
      }
    } else if (e.key === 'Escape') {
      showSuggestions = false;
      activeSuggestionIndex = -1;
    }
  }

  function selectSuggestion(suggestion) {
    searchQuery = suggestion.searchVal;
    showSuggestions = false;
    activeSuggestionIndex = -1;
    if (suggestion.id) {
      const doc = documents.find(d => d.id === suggestion.id);
      if (doc) openDocumentDetails(doc);
    }
  }

  function handleSaveSearch() {
    if (!searchQuery.trim()) return;
    if (savedSearches.includes(searchQuery.trim())) {
      alert("Этот запрос уже сохранен!");
      return;
    }
    savedSearches = [...savedSearches, searchQuery.trim()];
    alert(`Поисковый запрос "${searchQuery}" сохранен в избранное!`);
    logUserAction("SAVE_SEARCH", `Saved search query: "${searchQuery}"`);
  }
</script>

<div class="min-h-screen bg-background text-on-surface font-body-md pb-24">
  <!-- TopAppBar -->
  <header class="fixed top-0 left-0 w-full z-50 flex justify-between items-center px-6 h-14 bg-surface border-b border-outline-variant shadow-sm">
    <div class="flex items-center gap-3">
      <span class="material-symbols-outlined text-primary text-2xl" aria-hidden="true">menu</span>
      <h1 class="font-headline-lg text-lg md:text-xl font-bold text-primary">DocArchive</h1>
    </div>

    <!-- Active Profile & Role switcher summary -->
    <div class="flex items-center gap-4">
      <div class="hidden sm:flex flex-col text-right">
        <span class="text-xs font-bold text-on-surface uppercase">{currentRole} View</span>
        <span class="text-[10px] text-on-surface-variant">ЦНИИ Эпидемиологии</span>
      </div>
      <button
        type="button"
        class="w-9 h-9 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container font-bold text-sm border border-outline-variant"
        on:click={() => currentTab = 'profile'}
        title="View Profile and Role Settings"
        aria-label="User Profile"
      >
        {currentRole.substring(0, 2)}
      </button>
    </div>
  </header>

  <!-- Main Scrollable Canvas -->
  <main class="pt-20 pb-20 px-4 md:px-6 max-w-4xl mx-auto space-y-6">

    <!-- Active Tab: SEARCH (KNOWLEDGE BASE) -->
    {#if currentTab === 'search'}

      <!-- Interactive Search & Filter Section -->
      <section class="mt-4 space-y-3" aria-labelledby="search-section-heading">
        <h2 id="search-section-heading" class="sr-only">Search and Filter Documents</h2>
        <div class="flex gap-2 items-center relative">

          <!-- Search input wrapper with ARIA attributes -->
          <div class="flex-grow flex items-center bg-white h-12 px-4 rounded-xl transition-all border border-outline-variant focus-within:border-secondary focus-within:ring-2 focus-within:ring-secondary/10 relative">
            <span class="material-symbols-outlined text-on-surface-variant mr-3" aria-hidden="true">search</span>
            <input
              type="text"
              class="bg-transparent border-none focus:ring-0 w-full text-body-md text-on-surface placeholder:text-on-surface-variant outline-none"
              placeholder="Search ФБУН, ГЭК, ГИА, ФГОС..."
              bind:value={searchQuery}
              on:input={() => showSuggestions = true}
              on:focus={() => showSuggestions = true}
              on:keydown={handleSearchKeyDown}
              role="combobox"
              aria-expanded={showSuggestions && suggestions.length > 0}
              aria-controls="search-suggestions-listbox"
              aria-autocomplete="list"
              id="search-input"
            />
            {#if searchQuery}
              <button
                type="button"
                class="text-on-surface-variant hover:text-primary p-1"
                on:click={() => { searchQuery = ''; showSuggestions = false; }}
                aria-label="Clear Search"
              >
                <span class="material-symbols-outlined text-sm">close</span>
              </button>
            {/if}
          </div>

          <!-- Save Search query button -->
          {#if searchQuery.trim()}
            <button
              type="button"
              class="h-12 px-3 flex items-center justify-center border border-outline-variant rounded-xl bg-white hover:bg-surface-container text-secondary transition-all"
              on:click={handleSaveSearch}
              title="Save current query"
              aria-label="Save current search"
            >
              <span class="material-symbols-outlined">bookmark_add</span>
            </button>
          {/if}

          <!-- Filters Toggle Button -->
          <button
            type="button"
            class="h-12 w-12 flex items-center justify-center border border-outline-variant rounded-xl bg-white hover:bg-surface-container transition-colors active:scale-95"
            on:click={() => showFilterModal = !showFilterModal}
            aria-label="Toggle filters"
            aria-expanded={showFilterModal}
            id="filters-toggle-button"
          >
            <span class="material-symbols-outlined text-on-surface {showFilterModal || selectedDocType !== 'ALL' || selectedSpecialty !== 'ALL' || selectedLevel !== 'ALL' ? 'text-secondary font-bold' : ''}">tune</span>
          </button>

          <!-- Auto-Suggestions Pop-over Dropdown -->
          {#if showSuggestions && suggestions.length > 0}
            <div class="absolute top-14 left-0 w-full bg-white border border-outline-variant rounded-xl shadow-lg z-50 overflow-hidden">
              <ul
                id="search-suggestions-listbox"
                role="listbox"
                aria-label="Search Suggestions"
                class="divide-y divide-outline-variant"
              >
                {#each suggestions as sug, i}
                  <li role="presentation">
                    <button
                      type="button"
                      role="option"
                      aria-selected={i === activeSuggestionIndex}
                      class="w-full text-left p-3 flex items-center justify-between transition-colors text-sm focus:outline-none
                        {i === activeSuggestionIndex ? 'bg-secondary-container/10 text-secondary font-semibold' : 'hover:bg-surface-container-low text-on-surface'}"
                      on:click={() => selectSuggestion(sug)}
                    >
                      <div class="flex items-center gap-2">
                        <span class="material-symbols-outlined text-sm text-outline">
                          {sug.type === 'synonym' ? 'key_visualizer' : 'description'}
                        </span>
                        <span>{sug.text}</span>
                      </div>
                      <span class="text-[10px] uppercase font-bold text-on-surface-variant px-1.5 py-0.5 bg-surface-container rounded shrink-0">
                        {sug.type}
                      </span>
                    </button>
                  </li>
                {/each}
              </ul>
            </div>
          {/if}

        </div>

        <!-- Filter Quick Summary / Active Filters Pills -->
        {#if selectedDocType !== 'ALL' || selectedSpecialty !== 'ALL' || selectedLevel !== 'ALL'}
          <div class="flex flex-wrap gap-2 items-center text-xs">
            <span class="text-on-surface-variant font-semibold">Active Filters:</span>
            {#if selectedDocType !== 'ALL'}
              <span class="bg-secondary-container/10 text-secondary border border-secondary-container/30 px-2 py-0.5 rounded-full flex items-center gap-1">
                Type: {selectedDocType}
                <button type="button" on:click={() => selectedDocType = 'ALL'}>&times;</button>
              </span>
            {/if}
            {#if selectedSpecialty !== 'ALL'}
              <span class="bg-secondary-container/10 text-secondary border border-secondary-container/30 px-2 py-0.5 rounded-full flex items-center gap-1">
                Spec: {selectedSpecialty}
                <button type="button" on:click={() => selectedSpecialty = 'ALL'}>&times;</button>
              </span>
            {/if}
            {#if selectedLevel !== 'ALL'}
              <span class="bg-secondary-container/10 text-secondary border border-secondary-container/30 px-2 py-0.5 rounded-full flex items-center gap-1">
                Level: {selectedLevel}
                <button type="button" on:click={() => selectedLevel = 'ALL'}>&times;</button>
              </span>
            {/if}
            <button type="button" class="text-error font-semibold hover:underline" on:click={() => { selectedDocType = 'ALL'; selectedSpecialty = 'ALL'; selectedLevel = 'ALL'; }}>
              Reset All
            </button>
          </div>
        {/if}

        <!-- Inline Filter Panel -->
        {#if showFilterModal}
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 grid grid-cols-1 sm:grid-cols-4 gap-4 shadow-sm text-xs md:text-sm">
            <div>
              <label for="filter-type" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Doc Type</label>
              <select id="filter-type" bind:value={selectedDocType} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="ALL">All Formats</option>
                <option value="PDF">PDF Documents</option>
                <option value="DOCX">Word DOCX</option>
                <option value="XLSX">Excel XLSX</option>
              </select>
            </div>
            <div>
              <label for="filter-spec" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Specialty</label>
              <select id="filter-spec" bind:value={selectedSpecialty} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="ALL">All Specialties</option>
                <option value="Эпидемиология">Эпидемиология</option>
                <option value="Инфекционные болезни">Инфекционные болезни</option>
                <option value="Педиатрия">Педиатрия</option>
              </select>
            </div>
            <div>
              <label for="filter-level" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Education Level</label>
              <select id="filter-level" bind:value={selectedLevel} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="ALL">All Levels</option>
                <option value="Ординатура">Ординатура</option>
                <option value="Аспирантура">Аспирантура</option>
                <option value="ДПО">ДПО (Повышение квалификации)</option>
              </select>
            </div>
            <div>
              <label for="sort-by" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Sort By</label>
              <select id="sort-by" bind:value={sortBy} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="relevance">Relevance</option>
                <option value="date">Update Date</option>
                <option value="title">Alphabetical (A-Z)</option>
              </select>
            </div>
          </div>
        {/if}
      </section>

      <!-- Recently Viewed section -->
      {#if recentlyViewedIds.length > 0}
        <section aria-labelledby="recently-viewed-heading" class="space-y-3">
          <div class="flex justify-between items-center">
            <h3 id="recently-viewed-heading" class="font-headline-md text-base md:text-lg font-bold text-on-surface">Recently Viewed</h3>
            <span class="text-[10px] font-bold text-outline uppercase tracking-wider">Scroll &rarr;</span>
          </div>

          <!-- Touch-friendly horizontal scroll container -->
          <div class="flex overflow-x-auto gap-4 pb-2 hide-scrollbar -mx-1 px-1 scroll-smooth">
            {#each recentlyViewedIds as id}
              {@const doc = documents.find(d => d.id === id)}
              {#if doc}
                <div
                  class="min-w-[155px] max-w-[155px] bg-white border border-outline-variant hover:border-secondary rounded-xl p-3 shadow-sm hover:shadow-md transition-all cursor-pointer flex flex-col justify-between"
                  on:click={() => openDocumentDetails(doc)}
                  on:keydown={(e) => e.key === 'Enter' && openDocumentDetails(doc)}
                  role="button"
                  tabindex="0"
                >
                  <div>
                    <div class="h-14 w-full rounded-lg bg-surface-container-low flex items-center justify-center mb-2 relative">
                      <span class="material-symbols-outlined text-2xl {doc.fileType === 'PDF' ? 'text-error' : doc.fileType === 'DOCX' ? 'text-secondary' : 'text-green-600'}">
                        {doc.fileType === 'PDF' ? 'picture_as_pdf' : doc.fileType === 'DOCX' ? 'description' : 'table_chart'}
                      </span>
                    </div>
                    <p class="font-bold text-xs text-on-surface line-clamp-2 leading-tight">{doc.title}</p>
                  </div>
                  <p class="text-[9px] text-outline uppercase tracking-widest mt-2">{doc.fileType} • {doc.fileSize}</p>
                </div>
              {/if}
            {/each}
          </div>
        </section>
      {/if}

      <!-- Suggested Documents vertical list -->
      <section aria-labelledby="suggested-docs-heading" class="space-y-3">
        <h3 id="suggested-docs-heading" class="font-headline-md text-base md:text-lg font-bold text-on-surface">
          {searchQuery ? 'Search Results' : 'Suggested Documents'}
        </h3>

        <div class="grid grid-cols-1 gap-3">
          {#if filteredDocuments.length === 0}
            <div class="text-center py-10 bg-white border border-outline-variant rounded-xl space-y-2">
              <span class="material-symbols-outlined text-4xl text-outline-variant">find_in_page</span>
              <p class="font-bold text-base">No Matching Documents Found</p>
              <p class="text-xs text-on-surface-variant">Try searching for alternative synonyms, e.g. "ФГОС", "ГЭК", "ГИА", "ФБУН".</p>
            </div>
          {:else}
            {#each filteredDocuments as doc}
              <div
                class="flex items-start p-4 bg-white border border-outline-variant hover:border-secondary rounded-xl shadow-sm hover:shadow-md transition-all cursor-pointer group gap-4 relative"
                on:click={() => openDocumentDetails(doc)}
                on:keydown={(e) => e.key === 'Enter' && openDocumentDetails(doc)}
                role="button"
                tabindex="0"
              >
                <!-- File Type Icon Indicator -->
                <div class="w-11 h-11 flex-shrink-0 flex items-center justify-center rounded-lg
                  {doc.fileType === 'PDF' ? 'bg-red-50 text-error' : doc.fileType === 'DOCX' ? 'bg-blue-50 text-secondary' : 'bg-green-50 text-green-700'}">
                  <span class="material-symbols-outlined text-2xl">
                    {doc.fileType === 'PDF' ? 'picture_as_pdf' : doc.fileType === 'DOCX' ? 'description' : 'table_chart'}
                  </span>
                </div>

                <div class="flex-grow space-y-1 min-w-0">
                  <div class="flex justify-between items-start gap-2">
                    <h4 class="font-headline-md text-sm md:text-base font-bold text-on-surface group-hover:text-secondary transition-colors line-clamp-1">
                      {doc.title}
                    </h4>

                    <div class="flex gap-1 items-center shrink-0">
                      <!-- Favorite Star Toggle -->
                      <button
                        type="button"
                        class="p-1 hover:bg-surface-container rounded-full animate-pulse"
                        on:click|stopPropagation={() => {
                          doc.isFavorite = !doc.isFavorite;
                          documents = [...documents];
                          logUserAction("TOGGLE_FAVORITE", `${doc.isFavorite ? 'Favorited' : 'Unfavorited'} document ID: ${doc.id}`);
                        }}
                        aria-label="Toggle Favorite"
                      >
                        <span class="material-symbols-outlined text-lg {doc.isFavorite ? 'text-amber-500 fill-1' : 'text-outline-variant'}" style="font-variation-settings: 'FILL' {doc.isFavorite ? '1' : '0'};">star</span>
                      </button>

                      <!-- Role Specific Actions for quick delete (Content Manager or Admin) -->
                      {#if currentRole === 'ADMINISTRATOR' || currentRole === 'CONTENT_MANAGER'}
                        <button
                          type="button"
                          class="p-1 text-error hover:bg-red-50 rounded-full"
                          on:click|stopPropagation={() => deleteDocument(doc.id)}
                          aria-label="Delete document"
                          title="Delete Document"
                        >
                          <span class="material-symbols-outlined text-lg">delete</span>
                        </button>
                      {/if}
                    </div>
                  </div>

                  <p class="text-xs text-on-surface-variant line-clamp-1">
                    {doc.description}
                  </p>

                  <div class="flex flex-wrap gap-2 items-center pt-1">
                    <span class="px-1.5 py-0.5 bg-surface-container-high rounded text-[9px] font-mono font-bold tracking-wider uppercase text-outline">
                      {doc.fileType}
                    </span>
                    <span class="text-[10px] text-outline-variant">•</span>
                    <span class="text-[10px] font-semibold text-on-surface-variant">{doc.fileSize}</span>
                    <span class="text-[10px] text-outline-variant">•</span>
                    <span class="text-[10px] text-secondary font-medium">Updated: {doc.updatedAt.substring(0, 10)}</span>
                  </div>

                  <!-- Tag chips -->
                  <div class="flex flex-wrap gap-1.5 pt-1.5">
                    {#each doc.tags as tag}
                      <span class="bg-surface-container-low text-on-surface-variant border border-outline-variant/35 px-2 py-0.5 rounded text-[9px] font-medium">
                        #{tag}
                      </span>
                    {/each}
                  </div>
                </div>
              </div>
            {/each}
          {/if}
        </div>
      </section>

      <!-- Knowledge Base empty suggestion cues / stats info -->
      <section class="border-2 border-dashed border-outline-variant rounded-xl p-6 text-center bg-surface-container-low">
        <span class="material-symbols-outlined text-outline text-4xl mb-2" aria-hidden="true">school</span>
        <h4 class="font-headline-md text-sm md:text-base font-bold text-on-surface">FBUN Centralized Educational Hub</h4>
        <p class="text-xs text-on-surface-variant max-w-md mx-auto mt-1">
          Supports residency, postgrad, and continuing professional education in Epidemiology, Infectious Diseases, and Pediatrics.
        </p>
        <div class="mt-4 flex flex-wrap justify-center gap-3">
          <button
            type="button"
            class="bg-primary text-on-primary text-xs font-bold uppercase tracking-wider px-4 py-2.5 rounded-full hover:bg-slate-800 transition-all shadow-sm"
            on:click={() => { searchQuery = 'ФГОС'; showSuggestions = true; }}
          >
            ФГОС Standards
          </button>
          <button
            type="button"
            class="bg-surface-container text-secondary text-xs font-bold uppercase tracking-wider px-4 py-2.5 rounded-full border border-outline-variant hover:bg-surface-container-high transition-all"
            on:click={() => { searchQuery = 'ГИА'; showSuggestions = true; }}
          >
            ГИА Materials
          </button>
        </div>
      </section>

    <!-- Active Tab: MOODLE FINANCIAL (NEW) -->
    {:else if currentTab === 'moodle'}
      <section class="space-y-6" aria-labelledby="moodle-dashboard-heading">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-outline-variant">
          <div>
            <h2 id="moodle-dashboard-heading" class="font-headline-lg text-lg md:text-xl font-bold text-primary">Moodle Dashboard Automation</h2>
            <p class="text-xs text-on-surface-variant">Automate structure mapping, deploy core activity modules (Books, Pages), and manage frameworks.</p>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <span class="text-[11px] font-bold text-outline-variant uppercase">Current Perspective:</span>
            <span class="bg-primary/10 text-primary border border-primary/20 px-2.5 py-1 rounded text-xs font-bold font-mono">
              {currentRole}
            </span>
          </div>
        </div>

        <!-- Moodle Dashboard Configuration & Automation Control Panel -->
        <div class="bg-white border border-outline-variant rounded-xl p-5 space-y-4 shadow-sm">
          <div class="flex items-center gap-2 text-primary">
            <span class="material-symbols-outlined text-secondary" aria-hidden="true">terminal</span>
            <h3 class="font-title-lg text-base font-bold">Moodle Dashboard Configuration Script</h3>
          </div>

          <p class="text-xs text-on-surface-variant">
            Write configuration scripts that generate the required Pages, Books, and categories for the financial block, so that the structure is automated.
          </p>

          <div class="space-y-3">
            <label for="moodle-script-editor" class="block font-bold text-[10px] text-on-surface-variant uppercase tracking-wider">Configuration Script (JSON)</label>
            <textarea
              id="moodle-script-editor"
              bind:value={moodleConfigScript}
              rows="8"
              class="w-full font-mono text-xs p-3 bg-slate-900 text-slate-100 rounded-lg border border-outline-variant focus:outline-none focus:ring-2 focus:ring-secondary/50 focus:border-secondary"
              placeholder="Paste Moodle Configuration JSON here..."
              disabled={isConfiguring}
            ></textarea>
          </div>

          <!-- Execution Controls -->
          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              class="h-10 px-5 bg-secondary text-on-secondary-fixed hover:bg-opacity-90 active:scale-95 rounded-lg font-bold text-xs uppercase tracking-wider flex items-center gap-2 transition-all shadow-sm"
              on:click={executeMoodleScript}
              disabled={isConfiguring}
            >
              {#if isConfiguring}
                <span class="material-symbols-outlined text-sm animate-spin" aria-hidden="true">sync</span>
                Executing Automation...
              {:else}
                <span class="material-symbols-outlined text-sm" aria-hidden="true">play_arrow</span>
                Execute Configuration Script
              {/if}
            </button>

            <button
              type="button"
              class="h-10 px-4 bg-white hover:bg-slate-50 border border-outline-variant rounded-lg text-xs font-bold uppercase flex items-center gap-2"
              on:click={resetMoodleDashboard}
              disabled={isConfiguring}
            >
              <span class="material-symbols-outlined text-sm" aria-hidden="true">restart_alt</span>
              Reset Structure
            </button>
          </div>

          <!-- Animated Execution Console Log -->
          {#if isConfiguring || configureLogs.length > 0}
            <div class="space-y-2 mt-4 animate-fade-in">
              <div class="flex justify-between items-center text-xs font-bold text-on-surface-variant">
                <span>Execution Status Console</span>
                <span class="font-mono">{configureProgress}%</span>
              </div>

              <!-- Progress Bar -->
              <div class="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                <div class="bg-secondary h-full transition-all duration-200" style="width: {configureProgress}%"></div>
              </div>

              <!-- Terminal Block -->
              <div class="bg-slate-950 border border-slate-800 rounded-lg p-3 font-mono text-[11px] text-green-400 space-y-1 overflow-y-auto max-h-[160px] shadow-inner">
                {#each configureLogs as log}
                  <div class="leading-relaxed">
                    {#if log.includes('[SUCCESS]')}
                      <span class="text-green-300 font-bold">{log}</span>
                    {:else if log.includes('[ERROR]')}
                      <span class="text-red-400 font-bold">{log}</span>
                    {:else}
                      <span class="text-slate-300">{log}</span>
                    {/if}
                  </div>
                {/each}
              </div>
            </div>
          {/if}
        </div>

        <!-- Scaffolded UI Dashboard Section -->
        {#if isScaffolded && moodleParsedConfig}
          <div class="space-y-6 mt-6 animate-fade-in">
            <div class="border-t border-outline-variant pt-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
              <h3 class="font-headline-md text-base md:text-lg font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary" aria-hidden="true">dashboard_customize</span>
                Scaffolded Moodle Course: "{moodleParsedConfig.course || 'Financial Administration 2026'}"
              </h3>
              <span class="bg-green-100 text-green-700 border border-green-200 text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full">
                Automated Structure Live
              </span>
            </div>

            <!-- Categories and activity cards -->
            {#if moodleParsedConfig.categories}
              {#each moodleParsedConfig.categories as category}
                <div class="space-y-3 bg-white border border-outline-variant rounded-xl p-4 md:p-5 shadow-sm">
                  <div class="flex items-center justify-between border-b border-outline-variant pb-2">
                    <div>
                      <h4 class="font-title-lg text-sm md:text-base font-bold text-primary">{category.name}</h4>
                      {#if category.description}
                        <p class="text-[11px] text-on-surface-variant italic mt-0.5">{category.description}</p>
                      {/if}
                    </div>
                    <span class="font-label-sm text-[10px] font-semibold text-on-surface-variant uppercase tracking-wider bg-surface-container px-2 py-0.5 rounded">
                      {category.items ? category.items.length : 0} Items
                    </span>
                  </div>

                  <!-- Category Items -->
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {#if category.items}
                      {#each category.items as item}
                        {#if item.title === 'Risk Assessment'}
                          <!-- Locked Item -->
                          <div class="bg-surface-container-low opacity-75 border border-outline-variant border-dashed rounded-xl overflow-hidden cursor-not-allowed">
                            <div class="p-4 flex items-center gap-4">
                              <div class="w-12 h-12 rounded-lg bg-surface-variant flex items-center justify-center shrink-0">
                                <span class="material-symbols-outlined text-outline text-[24px]" aria-hidden="true">lock</span>
                              </div>
                              <div class="flex-grow min-w-0">
                                <div class="flex justify-between items-center gap-2">
                                  <h5 class="font-body-md text-xs font-semibold text-on-surface-variant truncate">{item.title}</h5>
                                  <span class="material-symbols-outlined text-outline text-xs" aria-hidden="true">lock</span>
                                </div>
                                <p class="text-on-surface-variant text-[10px] italic">Page • Prerequisite needed</p>
                              </div>
                            </div>
                          </div>
                        {:else}
                          <!-- Active Item -->
                          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden hover:border-secondary transition-all shadow-sm">
                            <div class="p-4 flex gap-4">
                              <div class="w-12 h-12 rounded-lg {item.type === 'Book' ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-high text-on-surface-variant'} flex items-center justify-center shrink-0">
                                <span class="material-symbols-outlined text-[24px]" aria-hidden="true">
                                  {item.type === 'Book' ? 'library_books' : 'description'}
                                </span>
                              </div>
                              <div class="flex-grow min-w-0 space-y-1">
                                <div class="flex justify-between items-start gap-2">
                                  <h5 class="font-body-md text-xs font-semibold text-primary truncate leading-tight">{item.title}</h5>
                                  {#if item.status === 'Completed'}
                                    <span class="material-symbols-outlined text-secondary text-sm" style="font-variation-settings: 'FILL' 1;" aria-hidden="true">check_circle</span>
                                  {/if}
                                </div>
                                <div class="flex items-center gap-1.5 flex-wrap">
                                  <span class="bg-primary/5 text-primary text-[9px] px-1 py-0.5 rounded font-bold uppercase tracking-wider border border-primary/15">{item.type}</span>
                                  {#if item.progress}
                                    <span class="text-on-surface-variant text-[10px] font-semibold">• {item.progress} Complete</span>
                                  {/if}
                                  {#if item.status === 'New'}
                                    <span class="bg-secondary text-on-secondary-fixed text-[8px] uppercase tracking-widest font-bold px-1.5 py-0.5 rounded">New</span>
                                  {/if}
                                </div>
                                {#if item.progress}
                                  <!-- Progress Bar -->
                                  <div class="w-full bg-slate-100 h-1.5 rounded-full mt-2 overflow-hidden">
                                    <div class="bg-secondary h-full rounded-full" style="width: {item.progress}"></div>
                                  </div>
                                {/if}
                              </div>
                            </div>
                          </div>
                        {/if}
                      {/each}
                    {/if}
                  </div>
                </div>
              {/each}
            {/if}

            <!-- TEACHER WORKLOAD VIEW SECTION -->
            {#if currentRole === 'TEACHER'}
              <div class="bg-white border-2 border-secondary/35 rounded-xl p-5 md:p-6 space-y-4 shadow-sm animate-fade-in">
                <div class="flex items-center justify-between border-b border-secondary/20 pb-2">
                  <div class="flex items-center gap-2">
                    <span class="material-symbols-outlined text-secondary text-2xl" aria-hidden="true">school</span>
                    <h4 class="font-title-lg text-sm md:text-base font-bold text-primary">Teacher Workload Frameworks & Guidelines</h4>
                  </div>
                  <span class="bg-secondary/15 text-secondary border border-secondary/30 text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
                    Official Rospotrebnadzor Standards
                  </span>
                </div>

                <p class="text-xs text-on-surface-variant leading-relaxed">
                  All educational staff must submit their monthly teaching hour allocations and clinical assignments aligned with Rospotrebnadzor standards by the 25th of each month.
                </p>

                <!-- Guideline framework chapters -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-3 pt-1">
                  <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant text-xs space-y-1">
                    <div class="flex items-center gap-1.5 text-primary font-bold">
                      <span class="material-symbols-outlined text-sm" aria-hidden="true">menu_book</span>
                      Chapter 1: Hour Allocations
                    </div>
                    <p class="text-[11px] text-on-surface-variant">Maximum teaching hours allowed per residency or postgraduate cycle.</p>
                  </div>
                  <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant text-xs space-y-1">
                    <div class="flex items-center gap-1.5 text-primary font-bold">
                      <span class="material-symbols-outlined text-sm" aria-hidden="true">clinical_notes</span>
                      Chapter 2: Clinical Assignments
                    </div>
                    <p class="text-[11px] text-on-surface-variant">Hours mapped to clinical diagnostics and epidemiology field inspections.</p>
                  </div>
                  <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant text-xs space-y-1">
                    <div class="flex items-center gap-1.5 text-primary font-bold">
                      <span class="material-symbols-outlined text-sm" aria-hidden="true">payments</span>
                      Chapter 3: Salary Rates
                    </div>
                    <p class="text-[11px] text-on-surface-variant">Base hourly compensation indexed by specialty and academic rank.</p>
                  </div>
                </div>

                <!-- Interactive Workload Table -->
                <div class="pt-3">
                  <h5 class="font-bold text-[10px] text-on-surface-variant uppercase tracking-wider mb-2">My Assigned Framework Workloads</h5>
                  <div class="border border-outline-variant rounded-xl overflow-hidden bg-slate-50/50">
                    <table class="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr class="bg-surface-container-low text-on-surface border-b border-outline-variant font-bold">
                          <th class="p-2 md:p-3">Specialty</th>
                          <th class="p-2 md:p-3">Teaching Hours</th>
                          <th class="p-2 md:p-3">Clinical Hours</th>
                          <th class="p-2 md:p-3">Salary Rate</th>
                          <th class="p-2 md:p-3">Status</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-outline-variant font-medium">
                        {#if moodleParsedConfig.workloads}
                          {#each moodleParsedConfig.workloads as workload}
                            <tr>
                              <td class="p-2 md:p-3 font-semibold text-primary">{workload.specialty}</td>
                              <td class="p-2 md:p-3">{workload.hours} hrs</td>
                              <td class="p-2 md:p-3">{workload.clinicalHours} hrs</td>
                              <td class="p-2 md:p-3 text-secondary font-mono">{workload.salaryRate}</td>
                              <td class="p-2 md:p-3">
                                <span class="bg-green-100 text-green-700 text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full">
                                  Approved
                                </span>
                              </td>
                            </tr>
                          {/each}
                        {/if}
                      </tbody>
                    </table>
                  </div>
                </div>

                <!-- Template Download Block -->
                <div class="p-4 bg-secondary-container/10 border border-secondary-container/30 rounded-xl mt-3 space-y-3">
                  <h5 class="font-bold text-xs text-secondary uppercase tracking-wider flex items-center gap-1">
                    <span class="material-symbols-outlined text-sm" aria-hidden="true">download</span>
                    Required Templates & Submission Packages
                  </h5>
                  <p class="text-[11px] text-on-surface-variant">Click to download standard documents required for teaching hour framework submission.</p>

                  <div class="flex flex-wrap gap-2 pt-1">
                    <a
                      href="#download-excel"
                      class="h-9 px-4 bg-white hover:bg-slate-50 border border-outline-variant rounded-lg text-xs font-bold text-primary uppercase flex items-center gap-2 shadow-sm transition-all"
                      on:click|preventDefault={() => alert("Downloaded: Workload_Template_Framework.xlsx")}
                    >
                      <span class="material-symbols-outlined text-sm text-green-600" aria-hidden="true">table_chart</span>
                      Workload Template (XLSX)
                    </a>
                    <a
                      href="#download-docx"
                      class="h-9 px-4 bg-white hover:bg-slate-50 border border-outline-variant rounded-lg text-xs font-bold text-primary uppercase flex items-center gap-2 shadow-sm transition-all"
                      on:click|preventDefault={() => alert("Downloaded: Workload_Submission_Guidelines.docx")}
                    >
                      <span class="material-symbols-outlined text-sm text-blue-600" aria-hidden="true">description</span>
                      Guidelines Guide (DOCX)
                    </a>
                  </div>
                </div>
              </div>
            {/if}
          </div>
        {:else}
          <!-- Empty State -->
          <div class="border-2 border-dashed border-outline-variant rounded-xl p-8 text-center bg-surface-container-low max-w-md mx-auto mt-6 animate-fade-in">
            <span class="material-symbols-outlined text-outline-variant text-5xl mb-3" aria-hidden="true">construction</span>
            <h4 class="font-headline-md text-sm md:text-base font-bold text-on-surface">No Structure Scaffolded Yet</h4>
            <p class="text-xs text-on-surface-variant mt-1.5 leading-relaxed">
              Execute the configuration script above to automatically deploy and verify the financial reporting block categories, books, and pages.
            </p>
          </div>
        {/if}
      </section>

    <!-- Active Tab: LMS SYNC (PRESERVED) -->
    {:else}
      <!-- Let's check which tab is selected and render appropriately -->
      {#if currentTab === 'lms'}
        <section class="space-y-6" aria-labelledby="lms-sync-heading">
          <div class="flex justify-between items-center">
            <h2 id="lms-sync-heading" class="font-headline-lg text-lg md:text-xl font-bold text-secondary">Canvas LMS Sync Dashboard</h2>
            <div class="flex items-center gap-2 bg-surface-container border border-outline-variant rounded-lg p-1 text-xs">
              <button
                type="button"
                class="px-2 py-1 rounded font-semibold {syncStatus === 'success' ? 'bg-green-600 text-white' : 'text-on-surface hover:bg-surface-variant'}"
                on:click={() => syncStatus = 'success'}
              >
                Mock Success
              </button>
              <button
                type="button"
                class="px-2 py-1 rounded font-semibold {syncStatus === 'failed' ? 'bg-red-600 text-white' : 'text-on-surface hover:bg-surface-variant'}"
                on:click={() => syncStatus = 'failed'}
              >
                Mock Failed
              </button>
            </div>
          </div>

          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 flex flex-col items-center justify-center text-center relative overflow-hidden">
            {#if syncStatus === 'syncing'}
              <div class="absolute top-0 left-0 h-1 bg-secondary transition-all duration-300" style="width: {syncProgress}%"></div>
              <div class="w-16 h-16 rounded-full bg-blue-100/30 flex items-center justify-center mb-4 border border-blue-200">
                <span class="material-symbols-outlined text-secondary text-3xl animate-spin">sync</span>
              </div>
              <h3 class="font-headline-lg text-lg text-on-background mb-1">Synchronizing Node...</h3>
              <p class="text-on-surface-variant text-xs mb-6">Updating roles and metadata maps ({syncProgress}%)</p>
            {:else if syncStatus === 'failed'}
              <div class="absolute top-0 left-0 w-full h-1 bg-error"></div>
              <div class="w-16 h-16 rounded-full bg-red-100/30 flex items-center justify-center mb-4 border border-red-300">
                <span class="material-symbols-outlined text-error text-3xl">error</span>
              </div>
              <h3 class="font-headline-lg text-lg text-on-background mb-1">Synchronization Failed</h3>
              <p class="text-on-surface-variant text-xs mb-4">LMS failed to sync with upstream node</p>

              <div class="w-full bg-error-container text-on-error-container p-3 rounded-lg mb-4 text-left border border-red-200 text-xs" role="alert">
                <p class="font-semibold flex items-center gap-1">
                  <span class="material-symbols-outlined text-xs">warning</span>
                  Sync Failure Details:
                </p>
                <p class="font-mono mt-1 text-[11px]">{syncErrorMessage}</p>
              </div>

              <button
                type="button"
                class="w-full max-w-xs h-10 bg-error text-white hover:bg-red-700 active:scale-95 rounded-lg font-semibold uppercase text-xs flex items-center justify-center gap-2 transition-all"
                on:click={startSync}
              >
                <span class="material-symbols-outlined text-sm">replay</span>
                Retry Sync
              </button>
            {:else}
              <div class="absolute top-0 left-0 w-full h-1 bg-green-500"></div>
              <div class="w-16 h-16 rounded-full bg-green-100/30 flex items-center justify-center mb-4 border border-green-200">
                <span class="material-symbols-outlined text-green-600 text-3xl">check_circle</span>
              </div>
              <h3 class="font-headline-lg text-lg text-on-background mb-1">System Operational</h3>
              <p class="text-on-surface-variant text-xs mb-6">Last sync successful at {lastLmsSync}</p>

              <button
                type="button"
                class="w-full max-w-xs h-10 bg-primary text-on-primary hover:opacity-90 active:scale-95 rounded-lg font-semibold uppercase text-xs flex items-center justify-center gap-2 transition-all"
                on:click={startSync}
              >
                <span class="material-symbols-outlined text-sm">sync</span>
                Sync Now
              </button>
            {/if}
          </div>

          <!-- Connection stats -->
          <div class="grid grid-cols-2 gap-4">
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4">
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Synced Records</p>
              <p class="text-2xl font-mono font-bold text-secondary mt-1">{syncedRecords}</p>
            </div>
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4">
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Connection Profile</p>
              <p class="text-sm font-semibold mt-2">{connectionProfile}</p>
            </div>
          </div>
        </section>

      {:else if currentTab === 'messenger'}
        <section class="space-y-4" aria-labelledby="messenger-subs-heading">
          <div class="flex justify-between items-center">
            <div>
              <h2 id="messenger-subs-heading" class="font-headline-lg text-lg md:text-xl font-bold text-primary">Messenger Subscriptions</h2>
              <p class="text-xs text-on-surface-variant">Notify system administrators, content managers, and teachers of sync events.</p>
            </div>
            <button
              type="button"
              class="px-3 py-2 bg-secondary text-white hover:bg-blue-700 rounded-lg text-xs font-semibold flex items-center gap-1 transition-all"
              on:click={() => showAddForm = !showAddForm}
            >
              <span class="material-symbols-outlined text-sm">{showAddForm ? 'close' : 'add'}</span>
              {showAddForm ? 'Cancel' : 'New Sub'}
            </button>
          </div>

          {#if showAddForm}
            <form on:submit|preventDefault={addSubscription} class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 space-y-3">
              <h3 class="font-bold text-xs uppercase text-primary tracking-wider">Register Subscription Channel</h3>
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                <div>
                  <label for="user-role" class="block text-[10px] font-bold text-on-surface-variant uppercase mb-1">Target EIOS Role</label>
                  <select id="user-role" bind:value={newUserId} class="w-full bg-background border border-outline-variant rounded-lg p-2 focus:outline-none focus:border-secondary">
                    <option value="ADMINISTRATOR">ADMINISTRATOR</option>
                    <option value="CONTENT_MANAGER">CONTENT_MANAGER</option>
                    <option value="TEACHER">TEACHER</option>
                    <option value="LEARNER">LEARNER</option>
                  </select>
                </div>
                <div>
                  <label for="channel-id" class="block text-[10px] font-bold text-on-surface-variant uppercase mb-1">Channel ID</label>
                  <input type="text" id="channel-id" bind:value={newChannelId} placeholder="@channel_name" class="w-full bg-background border border-outline-variant rounded-lg p-2 focus:outline-none" required />
                </div>
                <div>
                  <label for="notif-type" class="block text-[10px] font-bold text-on-surface-variant uppercase mb-1">Notification Type</label>
                  <select id="notif-type" bind:value={newNotificationType} class="w-full bg-background border border-outline-variant rounded-lg p-2 focus:outline-none">
                    <option value="CRITICAL_ALERTS">CRITICAL_ALERTS</option>
                    <option value="SYNC_SUMMARY">SYNC_SUMMARY</option>
                    <option value="USER_UPDATES">USER_UPDATES</option>
                  </select>
                </div>
              </div>
              <div class="flex justify-end gap-2">
                <button type="submit" class="px-4 py-2 bg-primary text-on-primary rounded-lg font-bold text-xs">Register</button>
              </div>
            </form>
          {/if}

          <!-- Subscriptions List -->
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl divide-y divide-outline-variant overflow-hidden">
            {#each messengerSubscriptions as sub}
              <div class="p-4 flex justify-between items-center gap-4 text-xs">
                <div>
                  <p class="font-bold text-sm text-on-surface">{sub.channelOrChatId}</p>
                  <p class="text-[10px] text-on-surface-variant mt-1">Role: <span class="font-mono font-bold text-secondary">{sub.userId}</span> • Type: {sub.notificationType}</p>
                </div>
                <div class="flex items-center gap-3">
                  <button
                    type="button"
                    class="px-2.5 py-1 text-[10px] font-bold rounded uppercase {sub.isActive ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-500'}"
                    on:click={() => toggleSubscription(sub.id)}
                  >
                    {sub.isActive ? 'Active' : 'Muted'}
                  </button>
                  <button type="button" class="text-error p-1 hover:bg-red-50 rounded" on:click={() => deleteSubscription(sub.id)}>
                    <span class="material-symbols-outlined text-lg">delete</span>
                  </button>
                </div>
              </div>
            {/each}
          </div>
        </section>

      {:else if currentTab === 'profile'}
        <!-- Profile, Role Switcher and Administrative Tools -->
        <section class="space-y-6" aria-labelledby="profile-heading">
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6">
            <h2 id="profile-heading" class="font-headline-lg text-lg md:text-xl font-bold text-on-surface">EIOS User Profile</h2>
            <p class="text-xs text-on-surface-variant mt-1">System privileges and active roles are assigned globally to control Knowledge Base access.</p>

            <!-- Role Selector -->
            <div class="mt-6 p-4 bg-surface-container-low rounded-xl border border-outline-variant/50 space-y-3">
              <label for="role-selector" class="block font-bold text-xs uppercase tracking-wider text-secondary">Active Access Role Switcher</label>
              <select
                id="role-selector"
                bind:value={currentRole}
                class="w-full bg-white border border-outline-variant rounded-lg p-2.5 text-sm font-semibold focus:border-secondary focus:outline-none"
              >
                <option value="ADMINISTRATOR">ADMINISTRATOR (Full Rights, Audit, Section Setup, Backups)</option>
                <option value="CONTENT_MANAGER">CONTENT_MANAGER (Manage Documents, Edit Articles, Categories, Tags)</option>
                <option value="TEACHER">TEACHER / SUPERVISOR (Suggest edits, Collections, Browse All)</option>
                <option value="LEARNER">LEARNER / STUDENT (Search, Filter, Favorite, Subscribe to updates)</option>
              </select>
              <p class="text-[11px] text-on-surface-variant font-medium leading-relaxed mt-1">
                {#if currentRole === 'ADMINISTRATOR'}
                  <strong>Administrator capabilities enabled:</strong> Вы можете удалять разделы, настраивать роли, выгружать отчеты об использовании, запускать резервное копирование и просматривать системные логи безопасности.
                {:else}
                  <strong>{currentRole} capabilities enabled:</strong> Доступ ограничен соответствующим уровнем прав в соответствии с техническим заданием.
                {/if}
              </p>
            </div>
          </div>

          <!-- ROLE SPECIFIC WORKFLOWS -->
          {#if currentRole === 'ADMINISTRATOR'}
            <div class="bg-white border border-outline-variant rounded-xl p-6 space-y-4 shadow-sm">
              <h3 class="font-headline-md text-base font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary">shield</span>
                Administrator System Utilities
              </h3>

              <!-- Backup Utility -->
              <div class="p-4 border border-outline-variant/60 rounded-xl space-y-3 bg-surface-container-low/20">
                <h4 class="font-bold text-xs text-on-surface uppercase tracking-wider">System Backup Recovery</h4>
                <p class="text-xs text-on-surface-variant">Create an immediate backup copy of Knowledge Base documents metadata and synchronization records.</p>
                <div class="flex flex-wrap gap-3 items-center">
                  <button
                    type="button"
                    class="bg-secondary hover:bg-blue-700 text-white font-bold text-xs py-2 px-4 rounded-lg flex items-center gap-2"
                    on:click={triggerBackup}
                    disabled={isBackingUp}
                  >
                    <span class="material-symbols-outlined text-sm {isBackingUp ? 'animate-spin' : ''}">backup</span>
                    {isBackingUp ? 'Creating Backup...' : 'Trigger Backup'}
                  </button>
                  {#if backupStatus}
                    <span class="text-xs font-mono font-medium text-green-600 bg-green-50 px-2 py-1 rounded border border-green-100">{backupStatus}</span>
                  {/if}
                </div>
              </div>

              <!-- Export statistics -->
              <div class="p-4 border border-outline-variant/60 rounded-xl space-y-3 bg-surface-container-low/20">
                <h4 class="font-bold text-xs text-on-surface uppercase tracking-wider">Export utilization statistics</h4>
                <p class="text-xs text-on-surface-variant">Download report containing search queries count, document downloads and total viewing statistics.</p>
                <button
                  type="button"
                  class="bg-primary hover:bg-slate-800 text-on-primary font-bold text-xs py-2 px-4 rounded-lg flex items-center gap-2"
                  on:click={() => alert("Выгрузка статистики использования завершена! Файл 'kb_statistics_2026.csv' скачан.")}
                >
                  <span class="material-symbols-outlined text-sm">analytics</span>
                  Download Utilization Stats CSV
                </button>
              </div>

              <!-- Audit Trail Log (who, when, what changed) -->
              <div class="space-y-3">
                <h4 class="font-bold text-xs text-on-surface uppercase tracking-wider">Security Audit Trail (User Actions Logging)</h4>
                <div class="border border-outline-variant rounded-xl divide-y divide-outline-variant max-h-[220px] overflow-y-auto bg-surface-container-low/10">
                  {#each userActionLogs as log}
                    <div class="p-3 text-xs space-y-1">
                      <div class="flex justify-between items-center">
                        <span class="font-mono font-bold text-secondary uppercase">{log.user}</span>
                        <span class="text-[10px] text-on-surface-variant">{log.time}</span>
                      </div>
                      <p class="text-on-surface-variant"><span class="font-bold font-mono text-[10px] bg-surface-container px-1 rounded mr-1">{log.action}</span> {log.details}</p>
                    </div>
                  {/each}
                </div>
              </div>
            </div>

          {:else if currentRole === 'TEACHER'}
            <div class="bg-white border border-outline-variant rounded-xl p-6 space-y-4 shadow-sm">
              <h3 class="font-headline-md text-base font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary">class</span>
                Teacher / Scientific Supervisor Tools
              </h3>

              <!-- Collections System -->
              <div class="p-4 border border-outline-variant/60 rounded-xl space-y-3">
                <h4 class="font-bold text-xs text-on-surface uppercase tracking-wider">My Course Collections (Подборки для групп)</h4>
                <p class="text-xs text-on-surface-variant">Form custom document collections for specific groups of residents and postgraduate students.</p>

                <div class="space-y-2 mt-2">
                  {#each teacherCollections as coll}
                    <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant/40 flex justify-between items-center">
                      <div>
                        <p class="font-bold text-xs text-on-surface">{coll.name}</p>
                        <p class="text-[10px] text-on-surface-variant mt-0.5">Documents count: {coll.docs.length}</p>
                      </div>
                      <button type="button" class="text-secondary text-xs font-bold uppercase hover:underline" on:click={() => alert(`Просмотр подборки: "${coll.name}"`)}>Open</button>
                    </div>
                  {/each}
                </div>

                <!-- Create Collection Form -->
                <div class="pt-2 flex gap-2">
                  <input
                    type="text"
                    placeholder="Collection name, e.g. Семинар 3"
                    class="p-2 border border-outline-variant rounded-lg text-xs flex-grow"
                    bind:value={newCollectionName}
                  />
                  <button type="button" class="bg-primary text-on-primary px-3 rounded-lg text-xs font-bold uppercase" on:click={addCollection}>Create</button>
                </div>
              </div>
            </div>

          {:else if currentRole === 'LEARNER'}
            <div class="bg-white border border-outline-variant rounded-xl p-6 space-y-4 shadow-sm">
              <h3 class="font-headline-md text-base font-bold text-primary flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary">star</span>
                My Saved & Bookmarked Items
              </h3>

              <!-- Saved Searches -->
              <div class="space-y-2">
                <h4 class="font-bold text-xs text-on-surface uppercase tracking-wider">Saved Searches</h4>
                {#if savedSearches.length === 0}
                  <p class="text-xs text-on-surface-variant">No saved search queries yet.</p>
                {:else}
                  <div class="flex flex-wrap gap-2">
                    {#each savedSearches as query}
                      <button
                        type="button"
                        role="link"
                        class="bg-secondary-container/10 text-secondary border border-secondary-container/30 px-3 py-1 rounded-full text-xs flex items-center gap-2 cursor-pointer hover:bg-secondary-container/20 text-left focus:outline-none"
                        on:click={() => { searchQuery = query; currentTab = 'search'; }}
                      >
                        <span class="material-symbols-outlined text-xs">search</span>
                        {query}
                      </button>
                    {/each}
                  </div>
                {/if}
              </div>

              <!-- Notifications Subscriptions -->
              <div class="p-4 border border-outline-variant/60 rounded-xl space-y-2 bg-surface-container-low/20">
                <h4 class="font-bold text-xs text-on-surface uppercase tracking-wider">Update Subscriptions Alert</h4>
                <p class="text-xs text-on-surface-variant">Configure system-wide notifications about newly added residency and postgraduate documents directly to your account.</p>
                <button type="button" class="bg-secondary hover:bg-blue-700 text-white font-bold text-xs py-2 px-4 rounded-lg" on:click={() => alert("Вы подписались на все обновления нормативных актов!")}>
                  Subscribe to 'нормативные акты' Updates
                </button>
              </div>
            </div>
          {/if}
        </section>
      {/if}
    {/if}

  </main>

  <!-- --- DOCUMENT DETAILS DRAWER / BOTTOM SHEET OVERLAY --- -->
  {#if showDetailModal && selectedDocument}
    <div class="fixed inset-0 bg-black/60 z-50 flex items-end justify-center sm:items-center p-0 sm:p-4" role="dialog" aria-modal="true" aria-labelledby="detail-title">
      <div class="bg-white w-full max-w-2xl rounded-t-2xl sm:rounded-2xl max-h-[85vh] overflow-y-auto flex flex-col focus:outline-none shadow-2xl relative" tabindex="-1">

        <!-- Header -->
        <div class="p-5 border-b border-outline-variant flex justify-between items-start sticky top-0 bg-white z-10">
          <div class="space-y-1">
            <span class="text-[10px] font-bold text-secondary uppercase tracking-widest">{selectedDocument.category}</span>
            <h3 id="detail-title" class="font-headline-lg text-lg md:text-xl font-bold text-on-surface pr-8 leading-tight">
              {selectedDocument.title}
            </h3>
          </div>
          <button
            type="button"
            class="absolute top-4 right-4 w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container transition-colors"
            on:click={() => showDetailModal = false}
            aria-label="Close details"
          >
            <span class="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        <!-- Export Toast Notification -->
        {#if exportSuccessToast}
          <div class="bg-green-600 text-white p-3 text-center font-semibold text-xs relative z-20 animate-fade-in" role="status">
            {exportSuccessToast}
          </div>
        {/if}

        <!-- Body -->
        <div class="p-5 space-y-6 flex-grow">

          <!-- Metadata Grid -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs bg-surface-container-low p-4 rounded-xl border border-outline-variant/30">
            <div>
              <p class="text-on-surface-variant uppercase text-[9px] font-bold tracking-wider mb-1">File Format</p>
              <p class="font-bold text-sm text-secondary uppercase">{selectedDocument.fileType}</p>
            </div>
            <div>
              <p class="text-on-surface-variant uppercase text-[9px] font-bold tracking-wider mb-1">File Size</p>
              <p class="font-bold text-sm text-on-surface">{selectedDocument.fileSize}</p>
            </div>
            <div>
              <p class="text-on-surface-variant uppercase text-[9px] font-bold tracking-wider mb-1">Current Version</p>
              <p class="font-bold text-sm text-on-surface">v{selectedDocument.versionNumber}</p>
            </div>
            <div>
              <p class="text-on-surface-variant uppercase text-[9px] font-bold tracking-wider mb-1">Author</p>
              <p class="font-bold text-sm text-on-surface truncate">{selectedDocument.author}</p>
            </div>
          </div>

          <!-- Description -->
          <div class="space-y-1.5 text-xs md:text-sm">
            <h4 class="font-bold uppercase text-[10px] tracking-wider text-outline">Document Description</h4>
            <p class="text-on-surface-variant leading-relaxed">
              {selectedDocument.description}
            </p>
          </div>

          <!-- Interactive Action Buttons -->
          <div class="flex flex-wrap gap-2.5 pt-2">

            <!-- Export to PDF -->
            <button
              type="button"
              class="h-10 px-4 bg-secondary text-white rounded-lg hover:bg-blue-700 font-bold text-xs uppercase tracking-wider flex items-center gap-2 transition-all"
              on:click={() => simulateExport('PDF')}
              disabled={isExporting}
            >
              <span class="material-symbols-outlined text-sm">picture_as_pdf</span>
              {isExporting && exportType === 'PDF' ? 'Exporting...' : 'Export to PDF'}
            </button>

            <!-- Export to DOCX -->
            <button
              type="button"
              class="h-10 px-4 bg-slate-100 hover:bg-slate-200 text-on-surface border border-outline-variant rounded-lg font-bold text-xs uppercase tracking-wider flex items-center gap-2 transition-all"
              on:click={() => simulateExport('DOCX')}
              disabled={isExporting}
            >
              <span class="material-symbols-outlined text-sm">description</span>
              {isExporting && exportType === 'DOCX' ? 'Exporting...' : 'Export to Word'}
            </button>

            <!-- Bookmark / Subscribe / Request update depending on Role -->
            <button
              type="button"
              class="h-10 px-4 bg-white hover:bg-slate-50 border border-outline-variant rounded-lg text-xs font-bold uppercase flex items-center gap-2"
              on:click={toggleFavoriteInDetails}
            >
              <span class="material-symbols-outlined text-sm {selectedDocument.isFavorite ? 'text-amber-500 fill-1' : ''}" style="font-variation-settings: 'FILL' {selectedDocument.isFavorite ? '1' : '0'};">star</span>
              {selectedDocument.isFavorite ? 'In Favorites' : 'Add Favorite'}
            </button>

            <!-- Subscribe Updates -->
            <button
              type="button"
              class="h-10 px-4 bg-white hover:bg-slate-50 border border-outline-variant rounded-lg text-xs font-bold uppercase flex items-center gap-2"
              on:click={toggleSubscribeInDetails}
            >
              <span class="material-symbols-outlined text-sm {selectedDocument.isSubscribed ? 'text-green-600' : ''}">notifications</span>
              {selectedDocument.isSubscribed ? 'Subscribed' : 'Subscribe Alerts'}
            </button>

            <!-- Request Update Form Toggle -->
            <button
              type="button"
              class="h-10 px-4 bg-error-container text-on-error-container hover:opacity-90 rounded-lg text-xs font-bold uppercase flex items-center gap-2"
              on:click={() => showUpdateForm = !showUpdateForm}
            >
              <span class="material-symbols-outlined text-sm">update</span>
              Request Update
            </button>
          </div>

          <!-- Simulated Export progress bar -->
          {#if isExporting}
            <div class="space-y-1">
              <div class="flex justify-between text-xs font-semibold">
                <span>Generating {exportType} package...</span>
                <span>{exportProgress}%</span>
              </div>
              <div class="w-full bg-surface-container h-2 rounded-full overflow-hidden">
                <div class="bg-secondary h-full transition-all duration-150" style="width: {exportProgress}%"></div>
              </div>
            </div>
          {/if}

          <!-- Update Request Interactive Section -->
          {#if showUpdateForm}
            <form on:submit|preventDefault={submitUpdateRequest} class="p-4 border border-red-200 bg-error-container/10 rounded-xl space-y-3">
              <h5 class="font-bold text-xs uppercase text-error tracking-wider">Отправить запрос на актуализацию документа</h5>
              <p class="text-xs text-on-surface-variant">Укажите, какие разделы устарели, или предложите конкретные правки для следующей версии документа.</p>
              <textarea
                bind:value={updateRequestDesc}
                class="w-full p-2.5 border border-outline-variant rounded-lg text-xs md:text-sm bg-white"
                rows="3"
                placeholder="Пример: Обновился ФГОС на 2027 год, требуется заменить приложение..."
                required
              ></textarea>
              <div class="flex justify-end gap-2 text-xs">
                <button type="button" class="px-3 py-1.5 border border-outline-variant rounded-lg" on:click={() => showUpdateForm = false}>Отмена</button>
                <button type="submit" class="px-3 py-1.5 bg-error text-white rounded-lg font-bold">Отправить Запрос</button>
              </div>
            </form>
          {/if}

          <!-- Version History Table -->
          <div class="space-y-2">
            <h4 class="font-bold uppercase text-[10px] tracking-wider text-outline">Document Version History</h4>
            <div class="border border-outline-variant rounded-xl overflow-hidden bg-surface-container-lowest divide-y divide-outline-variant">
              {#each selectedDocument.history as hist}
                <div class="p-3 text-xs flex justify-between items-start gap-4">
                  <div class="space-y-1">
                    <p class="font-semibold text-on-surface">Version {hist.version} • <span class="text-secondary font-medium">{hist.author}</span></p>
                    <p class="text-on-surface-variant text-[11px]">{hist.desc}</p>
                  </div>
                  <span class="text-[10px] text-outline font-mono font-medium shrink-0">{hist.date}</span>
                </div>
              {/each}
            </div>
          </div>

          <!-- Comments Section -->
          <div class="space-y-3">
            <h4 class="font-bold uppercase text-[10px] tracking-wider text-outline">Document Comments & Revision Discussions</h4>

            <!-- List of existing comments -->
            <div class="space-y-2.5">
              {#if selectedDocument.comments.length === 0}
                <p class="text-xs text-on-surface-variant italic">No discussions or comments for this document yet.</p>
              {:else}
                {#each selectedDocument.comments as comment}
                  <div class="p-3 border border-outline-variant/65 rounded-xl bg-surface-container-low/20 space-y-1">
                    <div class="flex justify-between items-center text-[10px]">
                      <span class="font-bold text-secondary">{comment.author}</span>
                      <span class="text-on-surface-variant">{comment.date}</span>
                    </div>
                    <p class="text-xs text-on-surface leading-normal">{comment.text}</p>
                  </div>
                {/each}
              {/if}
            </div>

            <!-- Add Comment Form -->
            <form on:submit|preventDefault={handleCommentSubmit} class="flex gap-2 pt-1.5">
              <input
                type="text"
                bind:value={newCommentText}
                class="flex-grow p-2.5 border border-outline-variant rounded-xl text-xs md:text-sm bg-white"
                placeholder="Submit edit suggestion or comment..."
                required
              />
              <button type="submit" class="bg-primary text-on-primary px-4 rounded-xl text-xs font-bold uppercase tracking-wider hover:opacity-90 active:scale-95">Send</button>
            </form>
          </div>

        </div>
      </div>
    </div>
  {/if}

  <!-- --- FAB (UPLOAD NEW DOCUMENT MODAL) FOR CONTENT MANAGER / ADMIN --- -->
  {#if currentRole === 'ADMINISTRATOR' || currentRole === 'CONTENT_MANAGER'}
    <!-- Mobile FAB button placed inside bottom right -->
    <button
      type="button"
      class="fixed bottom-20 right-5 w-14 h-14 bg-primary text-on-primary rounded-full shadow-lg flex items-center justify-center active:scale-90 transition-transform z-40 hover:bg-slate-800"
      on:click={() => showUploadModal = true}
      aria-label="Upload New Document"
      title="Upload Document"
    >
      <span class="material-symbols-outlined text-[28px]">add</span>
    </button>
  {/if}

  <!-- Upload Document Modal -->
  {#if showUploadModal}
    <div class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="upload-modal-title">
      <div class="bg-white w-full max-w-lg rounded-2xl p-6 space-y-4 shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <h3 id="upload-modal-title" class="font-headline-lg text-lg font-bold text-primary flex items-center gap-2">
          <span class="material-symbols-outlined text-secondary">cloud_upload</span>
          Upload New Knowledge Base Document
        </h3>

        <button
          type="button"
          class="absolute top-4 right-4 w-9 h-9 flex items-center justify-center rounded-full hover:bg-surface-container"
          on:click={() => showUploadModal = false}
          aria-label="Close"
        >
          <span class="material-symbols-outlined text-xl">close</span>
        </button>

        <form on:submit|preventDefault={handleUploadSubmit} class="space-y-4 text-xs md:text-sm">
          <div>
            <label for="upload-title" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Document Title</label>
            <input
              type="text"
              id="upload-title"
              bind:value={newDocTitle}
              placeholder="e.g. Рабочая программа ординатуры Эпидемиология"
              class="w-full p-2.5 border border-outline-variant rounded-lg bg-white"
              required
            />
          </div>

          <div>
            <label for="upload-desc" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Brief Description</label>
            <textarea
              id="upload-desc"
              bind:value={newDocDesc}
              rows="3"
              placeholder="Provide a detailed roadmap, relevance, and details of this resource..."
              class="w-full p-2.5 border border-outline-variant rounded-lg bg-white"
              required
            ></textarea>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label for="upload-category" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Category</label>
              <select id="upload-category" bind:value={newDocCategory} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="нормативные акты">нормативные акты</option>
                <option value="шаблоны">шаблоны</option>
                <option value="вопросы к экзаменам">вопросы к экзаменам</option>
                <option value="рабочие программы">рабочие программы</option>
              </select>
            </div>
            <div>
              <label for="upload-type" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">File Type</label>
              <select id="upload-type" bind:value={newDocType} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="PDF">PDF Document</option>
                <option value="DOCX">Microsoft Word</option>
                <option value="XLSX">Microsoft Excel</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label for="upload-spec" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Specialty Focus</label>
              <select id="upload-spec" bind:value={newDocSpecialty} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="Эпидемиология">Эпидемиология</option>
                <option value="Инфекционные болезни">Инфекционные болезни</option>
                <option value="Педиатрия">Педиатрия</option>
              </select>
            </div>
            <div>
              <label for="upload-level" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Education Level</label>
              <select id="upload-level" bind:value={newDocLevel} class="w-full p-2 bg-background border border-outline-variant rounded-lg">
                <option value="Ординатура">Ординатура</option>
                <option value="Аспирантура">Аспирантура</option>
                <option value="ДПО">ДПО (Повышение квалификации)</option>
              </select>
            </div>
          </div>

          <div>
            <label for="upload-tags" class="block font-semibold text-on-surface-variant uppercase text-[10px] tracking-wider mb-1">Comma-separated Tags</label>
            <input
              type="text"
              id="upload-tags"
              bind:value={newDocTags}
              placeholder="e.g. ФГОС, ГИА, регламент"
              class="w-full p-2.5 border border-outline-variant rounded-lg bg-white"
            />
          </div>

          <div class="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              class="px-4 py-2 border border-outline-variant rounded-lg hover:bg-surface-container font-semibold"
              on:click={() => showUploadModal = false}
            >
              Cancel
            </button>
            <button
              type="submit"
              class="px-4 py-2 bg-primary text-on-primary rounded-lg font-bold uppercase tracking-wider hover:opacity-90 active:scale-95"
            >
              Confirm and Add
            </button>
          </div>
        </form>
      </div>
    </div>
  {/if}

  <!-- Unified Bottom Navigation Bar -->
  <nav class="fixed bottom-0 left-0 w-full z-50 flex justify-around items-center h-16 pb-safe bg-surface border-t border-outline-variant" aria-label="Main navigation">

    <!-- Search / Knowledge Base Tab -->
    <button
      type="button"
      class="flex flex-col items-center justify-center px-2 py-1 rounded-xl transition-all w-16
        {currentTab === 'search' ? 'text-secondary bg-secondary-container/10 font-bold' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'search'}
      id="tab-button-search"
    >
      <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' {currentTab === 'search' ? '1' : '0'};">school</span>
      <span class="font-label-caps text-[9px] font-semibold mt-1">Knowledge</span>
    </button>

    <!-- LMS Sync Tab -->
    <button
      type="button"
      class="flex flex-col items-center justify-center px-2 py-1 rounded-xl transition-all w-16
        {currentTab === 'lms' ? 'text-secondary bg-secondary-container/10' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'lms'}
      id="tab-button-lms"
    >
      <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' {currentTab === 'lms' ? '1' : '0'}; font-weight: {currentTab === 'lms' ? 'bold' : 'normal'};">sync_alt</span>
      <span class="font-label-caps text-[9px] font-semibold mt-1">LMS Sync</span>
    </button>

    <!-- Moodle Financial Tab -->
    <button
      type="button"
      class="flex flex-col items-center justify-center px-2 py-1 rounded-xl transition-all w-16
        {currentTab === 'moodle' ? 'text-secondary bg-secondary-container/10 font-bold' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'moodle'}
      id="tab-button-moodle"
    >
      <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' {currentTab === 'moodle' ? '1' : '0'}; font-weight: {currentTab === 'moodle' ? 'bold' : 'normal'};">dashboard</span>
      <span class="font-label-caps text-[9px] font-semibold mt-1">Moodle UI</span>
    </button>

    <!-- Messenger Tab -->
    <button
      type="button"
      class="flex flex-col items-center justify-center px-2 py-1 rounded-xl transition-all w-16
        {currentTab === 'messenger' ? 'text-secondary bg-secondary-container/10' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'messenger'}
      id="tab-button-messenger"
    >
      <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' {currentTab === 'messenger' ? '1' : '0'};">forum</span>
      <span class="font-label-caps text-[9px] font-semibold mt-1">Messenger</span>
    </button>

    <!-- Profile & Settings Tab -->
    <button
      type="button"
      class="flex flex-col items-center justify-center px-2 py-1 rounded-xl transition-all w-16
        {currentTab === 'profile' ? 'text-secondary bg-secondary-container/10' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'profile'}
      id="tab-button-profile"
    >
      <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' {currentTab === 'profile' ? '1' : '0'};">person</span>
      <span class="font-label-caps text-[9px] font-semibold mt-1">Profile</span>
    </button>
  </nav>
</div>

<style>
  /* Ensure smooth accessibility visual focus */
  button:focus-visible, input:focus-visible, select:focus-visible, textarea:focus-visible {
    outline: 3px solid #0058be !important;
    outline-offset: 2px !important;
  }

  /* Hide scrollbars for HORIZONTAL scrolling card decks */
  .hide-scrollbar::-webkit-scrollbar { display: none; }
  .hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

  /* Smooth animations */
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(-5px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fade-in {
    animation: fadeIn 0.3s ease-out forwards;
  }

  /* Material Symbols parameters */
  .fill-1 {
    font-variation-settings: 'FILL' 1 !important;
  }
</style>
