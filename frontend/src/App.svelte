<script>
  // Tabs: 'lms' or 'messenger'
  let currentTab = 'lms';

  // LMS Sync States
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
    },
    {
      id: 3,
      type: 'success',
      title: 'Incremental Update',
      time: '07:30:15',
      description: 'Web-hook triggered update for User UID:88291.',
      tags: []
    }
  ];

  // Messenger Subscriptions State
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
    },
    {
      id: 3,
      userId: 'TEACHER',
      channelOrChatId: '@eios_teachers_group',
      notificationType: 'USER_UPDATES',
      isActive: true,
      createdAt: '2026-07-30 23:14:15'
    }
  ];

  // Form State for new subscription
  let newUserId = 'ADMINISTRATOR';
  let newChannelId = '';
  let newNotificationType = 'CRITICAL_ALERTS';
  let showAddForm = false;

  // Sync Logic
  function startSync() {
    if (syncStatus === 'syncing') return;
    syncStatus = 'syncing';
    syncProgress = 0;

    let interval = setInterval(() => {
      if (syncProgress >= 100) {
        clearInterval(interval);
        syncStatus = 'success';
        // Update timestamps
        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const formattedTime = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
        const formattedDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${formattedTime}`;
        lastLmsSync = formattedDate;
        lastEiosSync = formattedDate;

        // Add success activity log
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
        syncProgress += Math.floor(Math.random() * 20) + 5;
        if (syncProgress > 100) syncProgress = 100;
      }
    }, 200);
  }

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

  // To easily toggle "failed" vs "success" state for review & testing
  function setMockState(state) {
    syncStatus = state;
    if (state === 'failed') {
      syncErrorMessage = 'Socket Connection Timeout: Failed to reach LMS endpoint within 10,000ms. Err: ETIMEDOUT';
    }
  }
</script>

<div class="min-h-screen bg-background text-on-surface font-body-md pb-24">
  <!-- TopAppBar -->
  <header class="fixed top-0 left-0 w-full z-50 flex justify-between items-center px-6 h-14 bg-surface border-b border-outline-variant">
    <div class="flex items-center gap-4">
      <button class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container-low transition-colors duration-200" aria-label="Go back">
        <span class="material-symbols-outlined text-on-surface">arrow_back</span>
      </button>
      <h1 class="font-headline-md text-xl md:text-2xl font-bold text-secondary">Canvas LMS Sync</h1>
    </div>
    <div class="flex items-center gap-3">
      <!-- Admin selector to simulate failed/success states easily -->
      <div class="hidden sm:flex bg-surface-container border border-outline-variant rounded-lg p-1 text-xs mr-2">
        <button
          type="button"
          class="px-2 py-1 rounded font-semibold {syncStatus === 'success' ? 'bg-green-600 text-white' : 'text-on-surface hover:bg-surface-variant'}"
          on:click={() => setMockState('success')}
        >
          Mock Success
        </button>
        <button
          type="button"
          class="px-2 py-1 rounded font-semibold {syncStatus === 'failed' ? 'bg-red-600 text-white' : 'text-on-surface hover:bg-surface-variant'}"
          on:click={() => setMockState('failed')}
        >
          Mock Failed
        </button>
      </div>

      <span class="material-symbols-outlined text-on-surface-variant" aria-hidden="true">settings</span>
      <div class="w-8 h-8 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container font-bold text-xs" title="Admin User">
        AD
      </div>
    </div>
  </header>

  <!-- Main Content Area -->
  <main class="pt-20 pb-24 px-4 md:px-6 max-w-4xl mx-auto space-y-6">

    {#if currentTab === 'lms'}
      <!-- LMS Sync Tab -->

      <!-- Status Hero Section -->
      <section class="grid grid-cols-1 md:grid-cols-3 gap-6" aria-labelledby="sync-status-heading">
        <h2 id="sync-status-heading" class="sr-only">LMS Sync Current Status</h2>

        <!-- Large Status Indicator -->
        <div class="md:col-span-2 bg-surface-container-lowest border border-outline-variant rounded-xl p-6 flex flex-col items-center justify-center text-center relative overflow-hidden">

          {#if syncStatus === 'syncing'}
            <div class="absolute top-0 left-0 h-1 bg-secondary transition-all duration-300" style="width: {syncProgress}%" aria-hidden="true"></div>
            <div class="w-24 h-24 rounded-full bg-blue-100/30 flex items-center justify-center mb-4 border border-blue-200 animate-pulse">
              <span class="material-symbols-outlined text-secondary text-5xl animate-spin">sync</span>
            </div>
            <h3 class="font-headline-lg text-2xl text-on-background mb-1">Synchronizing Node...</h3>
            <p class="text-on-surface-variant text-sm mb-6">Updating role maps, metadata entries, and logs</p>
            <button class="w-full max-w-xs h-11 bg-primary text-on-primary rounded-lg font-semibold uppercase tracking-wider flex items-center justify-center gap-2 opacity-50 cursor-not-allowed" disabled>
              <span class="material-symbols-outlined text-sm animate-spin">sync</span>
              Syncing ({syncProgress}%)
            </button>
          {:else if syncStatus === 'failed'}
            <div class="absolute top-0 left-0 w-full h-1 bg-error" aria-hidden="true"></div>
            <div class="w-24 h-24 rounded-full bg-red-100/30 flex items-center justify-center mb-4 border border-red-300">
              <span class="material-symbols-outlined text-error text-5xl" style="font-variation-settings: 'FILL' 1;">error</span>
            </div>
            <h3 class="font-headline-lg text-2xl text-on-background mb-1">Synchronization Failed</h3>
            <p class="text-on-surface-variant text-sm mb-4">Integrations failed to sync with upstream node</p>

            <!-- Clear Error Message for Failed Sync -->
            <div class="w-full bg-error-container text-on-error-container p-3 rounded-lg mb-6 text-left border border-red-200 text-sm" role="alert">
              <p class="font-semibold flex items-center gap-1">
                <span class="material-symbols-outlined text-sm" style="font-variation-settings: 'FILL' 1;">warning</span>
                Sync Failure Details:
              </p>
              <p class="font-mono mt-1 text-xs">{syncErrorMessage}</p>
            </div>

            <!-- Retry Button -->
            <button
              type="button"
              class="w-full max-w-xs h-11 bg-error text-white hover:bg-red-700 active:scale-95 rounded-lg font-semibold uppercase tracking-wider flex items-center justify-center gap-2 transition-all"
              on:click={startSync}
              id="retry-sync-button"
            >
              <span class="material-symbols-outlined text-sm">replay</span>
              Retry Sync
            </button>
          {:else}
            <!-- Success Status -->
            <div class="absolute top-0 left-0 w-full h-1 bg-green-500" aria-hidden="true"></div>
            <div class="w-24 h-24 rounded-full bg-green-100/30 flex items-center justify-center mb-4 border border-green-200">
              <span class="material-symbols-outlined text-green-600 text-5xl" style="font-variation-settings: 'FILL' 1;">check_circle</span>
            </div>
            <h3 class="font-headline-lg text-2xl text-on-background mb-1">System Operational</h3>
            <p class="text-on-surface-variant text-sm mb-6">Last sync successful at {lastLmsSync}</p>

            <button
              type="button"
              class="w-full max-w-xs h-11 bg-primary text-on-primary hover:opacity-90 active:scale-95 rounded-lg font-semibold uppercase tracking-wider flex items-center justify-center gap-2 transition-all"
              on:click={startSync}
              id="sync-now-button"
            >
              <span class="material-symbols-outlined text-sm">sync</span>
              Sync Now
            </button>
          {/if}

        </div>

        <!-- Stats Column -->
        <div class="flex flex-col gap-4">
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 flex flex-col justify-between min-h-[110px]">
            <p class="font-semibold text-xs tracking-wider text-on-surface-variant uppercase">Synced Records</p>
            <div class="mt-2">
              <span class="font-mono text-3xl font-bold text-secondary">{syncedRecords}</span>
              <div class="flex items-center gap-1 text-green-600 text-xs mt-1">
                <span class="material-symbols-outlined text-xs">trending_up</span>
                <span>+12 today</span>
              </div>
            </div>
          </div>
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 flex flex-col justify-between min-h-[110px]">
            <p class="font-semibold text-xs tracking-wider text-on-surface-variant uppercase">Avg Latency</p>
            <div class="mt-2">
              <span class="font-mono text-3xl font-bold text-on-surface">{avgLatency}</span>
              <p class="text-on-surface-variant text-xs mt-1">Stable connection</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Connection Profile Card -->
      <section class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden" aria-labelledby="connection-profile-heading">
        <div class="p-4 border-b border-outline-variant flex justify-between items-center bg-surface-container-low/50">
          <h3 id="connection-profile-heading" class="font-semibold text-xs tracking-wider text-on-surface-variant uppercase">Active Connection Profile</h3>
          <span class="px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 font-semibold text-[10px]">{connectionProfile}</span>
        </div>
        <div class="p-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <p class="text-xs text-on-surface-variant mb-1">Source Endpoint</p>
            <p class="font-mono text-xs truncate">{sourceEndpoint}</p>
          </div>
          <div>
            <p class="text-xs text-on-surface-variant mb-1">Auth Status</p>
            <div class="flex items-center gap-1">
              <div class="w-2 h-2 rounded-full bg-green-500"></div>
              <p class="font-semibold">{authStatus}</p>
            </div>
          </div>
          <div>
            <p class="text-xs text-on-surface-variant mb-1">Last LMS Sync Time</p>
            <p class="font-semibold text-xs text-secondary" id="last-lms-sync-time">{lastLmsSync}</p>
          </div>
          <div>
            <p class="text-xs text-on-surface-variant mb-1">Last EIOS Sync Time</p>
            <p class="font-semibold text-xs text-secondary" id="last-eios-sync-time">{lastEiosSync}</p>
          </div>
        </div>
      </section>

      <!-- Scrollable Recent Activity Logs -->
      <section class="space-y-3" aria-labelledby="recent-activity-heading">
        <div class="flex justify-between items-end px-1">
          <h3 id="recent-activity-heading" class="font-headline-md text-lg md:text-xl font-semibold text-on-background">Recent Sync Activity</h3>
          <button class="text-secondary font-semibold text-xs hover:underline">Download CSV</button>
        </div>

        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden">
          <div class="divide-y divide-outline-variant max-h-[350px] overflow-y-auto">

            {#if syncStatus === 'failed'}
              <!-- Error Log highlighted at top when failed -->
              <div class="flex items-start gap-4 p-4 hover:bg-surface-container-low transition-colors group bg-red-50/40">
                <div class="mt-1 w-8 h-8 rounded bg-error-container/20 flex items-center justify-center text-error shrink-0">
                  <span class="material-symbols-outlined text-lg">error</span>
                </div>
                <div class="flex-grow min-w-0">
                  <div class="flex justify-between items-center mb-1">
                    <span class="font-semibold text-on-surface text-sm md:text-base">Socket Connection Timeout</span>
                    <span class="font-mono text-xs text-on-surface-variant">04:11:59</span>
                  </div>
                  <p class="text-sm text-on-surface-variant mb-2">Failed to reach LMS endpoint within 10,000ms. Automatic retry in 5 minutes.</p>
                  <span class="px-2 py-0.5 bg-error-container text-on-error-container rounded text-[10px] font-mono font-semibold">Err: ETIMEDOUT</span>
                </div>
              </div>
            {/if}

            {#each activityLogs as log (log.id)}
              <div class="flex items-start gap-4 p-4 hover:bg-surface-container-low transition-colors group">
                <div class="mt-1 w-8 h-8 rounded shrink-0 flex items-center justify-center
                  {log.type === 'success' ? 'bg-green-50 text-green-600' : 'bg-amber-50 text-amber-600'}">
                  <span class="material-symbols-outlined text-lg">
                    {log.type === 'success' ? 'check' : 'warning'}
                  </span>
                </div>
                <div class="flex-grow min-w-0">
                  <div class="flex justify-between items-center mb-1">
                    <span class="font-semibold text-on-surface text-sm md:text-base">{log.title}</span>
                    <span class="font-mono text-xs text-on-surface-variant">{log.time}</span>
                  </div>
                  <p class="text-sm text-on-surface-variant mb-2">{log.description}</p>

                  {#if log.tags && log.tags.length > 0}
                    <div class="flex gap-2">
                      {#each log.tags as tag}
                        <span class="px-2 py-0.5 bg-surface-container-high rounded text-[10px] font-mono">{tag}</span>
                      {/each}
                    </div>
                  {/if}
                </div>
              </div>
            {/each}

          </div>
          <div class="p-3 text-center bg-surface-container-low/30 border-t border-outline-variant">
            <button class="text-on-surface-variant font-semibold text-xs hover:text-secondary transition-colors">Load Older Entries</button>
          </div>
        </div>
      </section>

    {:else}
      <!-- Messenger Subscriptions Tab -->
      <section class="space-y-4" aria-labelledby="messenger-subs-heading">
        <div class="flex justify-between items-center px-1">
          <div>
            <h2 id="messenger-subs-heading" class="font-headline-md text-xl md:text-2xl font-bold text-on-background">Messenger Subscriptions</h2>
            <p class="text-sm text-on-surface-variant">Configure notifications delivered to administrators, teachers, and content managers.</p>
          </div>
          <button
            type="button"
            class="px-3 py-2 bg-secondary text-white hover:bg-blue-700 rounded-lg text-xs font-semibold flex items-center gap-1 transition-all active:scale-95"
            on:click={() => showAddForm = !showAddForm}
            id="toggle-add-subscription-form"
          >
            <span class="material-symbols-outlined text-sm">{showAddForm ? 'close' : 'add'}</span>
            {showAddForm ? 'Cancel' : 'Add Channel'}
          </button>
        </div>

        <!-- Add New Subscription Form -->
        {#if showAddForm}
          <form on:submit|preventDefault={addSubscription} class="bg-surface-container-lowest border border-outline-variant rounded-xl p-5 space-y-4" id="add-subscription-form">
            <h3 class="font-semibold text-sm text-primary uppercase tracking-wider">New Subscription Channel</h3>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label for="user-role" class="block text-xs font-semibold text-on-surface-variant uppercase mb-1">Target EIOS Role</label>
                <select
                  id="user-role"
                  bind:value={newUserId}
                  class="w-full bg-background border border-outline-variant rounded-lg p-2 text-sm focus:outline-none focus:border-secondary"
                >
                  <option value="ADMINISTRATOR">ADMINISTRATOR</option>
                  <option value="CONTENT_MANAGER">CONTENT_MANAGER</option>
                  <option value="TEACHER">TEACHER</option>
                  <option value="LEARNER">LEARNER</option>
                </select>
              </div>

              <div>
                <label for="channel-id" class="block text-xs font-semibold text-on-surface-variant uppercase mb-1">Channel / Chat ID</label>
                <input
                  type="text"
                  id="channel-id"
                  bind:value={newChannelId}
                  placeholder="e.g. @eios_critical_alerts"
                  class="w-full bg-background border border-outline-variant rounded-lg p-2 text-sm focus:outline-none focus:border-secondary"
                  required
                />
              </div>

              <div>
                <label for="notif-type" class="block text-xs font-semibold text-on-surface-variant uppercase mb-1">Notification Type</label>
                <select
                  id="notif-type"
                  bind:value={newNotificationType}
                  class="w-full bg-background border border-outline-variant rounded-lg p-2 text-sm focus:outline-none focus:border-secondary"
                >
                  <option value="CRITICAL_ALERTS">CRITICAL_ALERTS</option>
                  <option value="SYNC_SUMMARY">SYNC_SUMMARY</option>
                  <option value="USER_UPDATES">USER_UPDATES</option>
                  <option value="COURSE_UPDATES">COURSE_UPDATES</option>
                </select>
              </div>
            </div>

            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                class="px-4 py-2 border border-outline-variant rounded-lg text-sm font-semibold hover:bg-surface-container"
                on:click={() => showAddForm = false}
              >
                Cancel
              </button>
              <button
                type="submit"
                class="px-4 py-2 bg-primary text-on-primary rounded-lg text-sm font-semibold hover:opacity-90 active:scale-95"
                id="submit-new-subscription"
              >
                Register Subscription
              </button>
            </div>
          </form>
        {/if}

        <!-- Subscriptions List -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden">
          <div class="divide-y divide-outline-variant">

            {#if messengerSubscriptions.length === 0}
              <div class="p-8 text-center text-on-surface-variant space-y-2">
                <span class="material-symbols-outlined text-4xl text-outline-variant">forum</span>
                <p class="font-semibold text-base">No messenger subscriptions found</p>
                <p class="text-xs">Configure notifications to receive immediate sync alerts.</p>
              </div>
            {:else}
              {#each messengerSubscriptions as sub (sub.id)}
                <div class="flex flex-col sm:flex-row sm:items-center justify-between p-4 gap-4 hover:bg-surface-container-low/30 transition-colors">
                  <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-secondary shrink-0">
                      <span class="material-symbols-outlined">forum</span>
                    </div>
                    <div>
                      <div class="flex items-center gap-2 flex-wrap">
                        <span class="font-semibold text-on-background text-sm md:text-base">{sub.channelOrChatId}</span>
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold tracking-wider uppercase bg-primary-fixed text-on-primary-fixed">{sub.userId}</span>
                      </div>
                      <p class="text-xs text-on-surface-variant mt-1">
                        Type: <span class="font-mono font-semibold">{sub.notificationType}</span> • Created: {sub.createdAt}
                      </p>
                    </div>
                  </div>

                  <div class="flex items-center gap-3 justify-end">
                    <!-- Active Status Toggle -->
                    <div class="flex items-center gap-2">
                      <span class="text-xs font-semibold uppercase {sub.isActive ? 'text-green-600' : 'text-on-surface-variant'}">
                        {sub.isActive ? 'Active' : 'Muted'}
                      </span>
                      <button
                        type="button"
                        class="w-12 h-6 rounded-full p-0.5 transition-colors duration-200 focus:outline-none
                          {sub.isActive ? 'bg-green-600' : 'bg-outline-variant'}"
                        on:click={() => toggleSubscription(sub.id)}
                        aria-label="Toggle active status for {sub.channelOrChatId}"
                        title="Toggle active status"
                      >
                        <div class="bg-white w-5 h-5 rounded-full shadow-md transform transition-transform duration-200
                          {sub.isActive ? 'translate-x-6' : 'translate-x-0'}"></div>
                      </button>
                    </div>

                    <!-- Delete Button -->
                    <button
                      type="button"
                      class="w-9 h-9 flex items-center justify-center text-error border border-outline-variant hover:bg-error-container/20 rounded-lg transition-colors"
                      on:click={() => deleteSubscription(sub.id)}
                      aria-label="Delete subscription for {sub.channelOrChatId}"
                      title="Delete subscription"
                    >
                      <span class="material-symbols-outlined text-lg">delete</span>
                    </button>
                  </div>
                </div>
              {/each}
            {/if}

          </div>
        </div>
      </section>
    {/if}

  </main>

  <!-- BottomNavBar -->
  <nav class="fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-4 py-2 bg-surface-container border-t border-outline-variant" aria-label="Main navigation">
    <button
      type="button"
      class="flex flex-col items-center justify-center px-3 py-1 rounded-xl transition-all
        {currentTab === 'lms' ? 'text-secondary bg-secondary-container/10' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'lms'}
      id="tab-button-lms"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {currentTab === 'lms' ? '1' : '0'};">sync_alt</span>
      <span class="font-label-caps text-xs font-semibold mt-1">LMS Sync</span>
    </button>

    <button
      type="button"
      class="flex flex-col items-center justify-center px-3 py-1 rounded-xl transition-all
        {currentTab === 'messenger' ? 'text-secondary bg-secondary-container/10' : 'text-on-surface-variant hover:text-primary'}"
      on:click={() => currentTab = 'messenger'}
      id="tab-button-messenger"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {currentTab === 'messenger' ? '1' : '0'};">forum</span>
      <span class="font-label-caps text-xs font-semibold mt-1">Messenger</span>
    </button>
  </nav>
</div>
