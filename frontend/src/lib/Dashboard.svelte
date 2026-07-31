<script>
  let headerScrolled = $state(false);

  function handleScroll() {
      headerScrolled = window.scrollY > 20;
  }
</script>

<svelte:window on:scroll={handleScroll} />

<!-- Top App Bar -->
<header
  class="flex justify-between items-center px-md py-base w-full sticky top-0 z-50 bg-surface border-b border-outline-variant transition-all duration-200"
  class:shadow-md={headerScrolled}
  class:bg-opacity-95={headerScrolled}
  class:backdrop-blur-md={headerScrolled}
>
  <div class="flex items-center gap-sm">
      <button
          class="material-symbols-outlined text-primary p-xs hover:bg-surface-container-high transition-colors active:scale-95 duration-150 rounded-full"
          aria-label="Open menu"
      >
          menu
      </button>
      <h1 class="font-headline-md text-headline-md font-bold text-primary">Financial Education</h1>
  </div>
  <button
      class="material-symbols-outlined text-primary p-xs hover:bg-surface-container-high transition-colors active:scale-95 duration-150 rounded-full"
      aria-label="Search"
  >
      search
  </button>
</header>

<!-- Category Section -->
<section class="mt-base" aria-label="Categories">
  <div class="flex overflow-x-auto gap-sm px-md py-sm hide-scrollbar" role="tablist">
      <button
          role="tab"
          aria-selected="true"
          class="whitespace-nowrap px-md py-xs rounded-full bg-primary-container text-on-primary-container font-label-md text-label-md transition-all active:scale-90"
      >
          All
      </button>
      <button
          role="tab"
          aria-selected="false"
          class="whitespace-nowrap px-md py-xs rounded-full bg-surface-container-low border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-container-high transition-all active:scale-90"
      >
          Investment
      </button>
      <button
          role="tab"
          aria-selected="false"
          class="whitespace-nowrap px-md py-xs rounded-full bg-surface-container-low border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-container-high transition-all active:scale-90"
      >
          Debt Management
      </button>
      <button
          role="tab"
          aria-selected="false"
          class="whitespace-nowrap px-md py-xs rounded-full bg-surface-container-low border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-container-high transition-all active:scale-90"
      >
          Retirement Planning
      </button>
  </div>
</section>

<!-- Main Content Canvas -->
<main class="px-md mt-base space-y-lg md:grid md:grid-cols-2 md:gap-lg md:space-y-0 lg:grid-cols-3 max-w-container-max mx-auto">
  <!-- Category: Basics of Wealth -->
  <section class="space-y-sm col-span-1" aria-labelledby="basics-heading">
      <div class="flex items-center justify-between border-b border-outline-variant pb-xs">
          <h2 id="basics-heading" class="font-title-lg text-title-lg text-primary">Basics of Wealth</h2>
          <span class="font-label-sm text-label-sm text-on-surface-variant" aria-label="2 items">2 ITEMS</span>
      </div>

      <!-- Item: ФГОС -->
      <button class="w-full text-left bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden active:scale-[0.98] transition-transform duration-150 shadow-sm cursor-pointer" aria-label="ФГОС высшего образования, Актуально">
          <div class="p-md flex gap-md">
              <div class="w-12 h-12 rounded-lg bg-primary-container flex items-center justify-center shrink-0" aria-hidden="true">
                  <span class="material-symbols-outlined text-on-primary-container text-2xl">gavel</span>
              </div>
              <div class="flex-1 space-y-xs">
                  <div class="flex justify-between items-start">
                      <h3 class="font-body-md text-body-md font-semibold text-primary leading-tight">ФГОС высшего образования</h3>
                      <span class="material-symbols-outlined text-on-surface-variant text-sm" aria-hidden="true">more_vert</span>
                  </div>
                  <div class="flex items-center gap-xs">
                      <span class="bg-on-primary-container/10 text-primary px-xs rounded text-[0.625rem] font-bold uppercase tracking-wider">PDF</span>
                      <span class="text-on-surface-variant font-label-sm text-label-sm">• Актуально</span>
                  </div>
              </div>
          </div>
      </button>

      <!-- Item: Приказы Минобрнауки -->
      <button class="w-full text-left bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden active:scale-[0.98] transition-transform duration-150 shadow-sm cursor-pointer" aria-label="Приказы Минобрнауки, Завершено">
          <div class="p-md flex items-center gap-md">
              <div class="w-12 h-12 rounded-lg bg-surface-container-high flex items-center justify-center shrink-0" aria-hidden="true">
                  <span class="material-symbols-outlined text-on-surface-variant text-2xl">description</span>
              </div>
              <div class="flex-1">
                  <div class="flex justify-between items-center">
                      <h3 class="font-body-md text-body-md font-medium text-on-surface">Приказы Минобрнауки</h3>
                      <span class="material-symbols-outlined text-secondary" style="font-variation-settings: 'FILL' 1;" aria-label="Актуально">check_circle</span>
                  </div>
                  <p class="text-on-surface-variant font-label-sm text-label-sm">Сборник • Обновлен сегодня</p>
              </div>
          </div>
      </button>
  </section>

  <!-- Category: Подготовка к аттестациям -->
  <section class="space-y-sm col-span-1" aria-labelledby="attestation-heading">
      <div class="flex items-center justify-between border-b border-outline-variant pb-xs">
          <h2 id="attestation-heading" class="font-title-lg text-title-lg text-primary">Подготовка к аттестациям</h2>
          <span class="font-label-sm text-label-sm text-on-surface-variant" aria-label="2 документа">2 ДОКУМЕНТА</span>
      </div>

      <!-- Item: Вопросы ГИА -->
      <button class="w-full text-left bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden active:scale-[0.98] transition-transform duration-150 shadow-sm relative cursor-pointer" aria-label="Вопросы ГИА (Эпидемиология), Новое">
          <div class="absolute top-0 right-0">
              <div class="bg-secondary px-sm py-xs rounded-bl-xl">
                  <span class="text-white font-label-sm text-[0.625rem] uppercase tracking-widest font-bold">New</span>
              </div>
          </div>
          <div class="p-md flex gap-md">
              <div class="w-12 h-12 rounded-lg bg-primary-container flex items-center justify-center shrink-0" aria-hidden="true">
                  <span class="material-symbols-outlined text-on-primary-container text-2xl">quiz</span>
              </div>
              <div class="flex-1 space-y-xs pr-base">
                  <h3 class="font-body-md text-body-md font-semibold text-primary leading-tight">Вопросы ГИА (Эпидемиология)</h3>
                  <p class="text-on-surface-variant font-label-sm text-label-sm">Документ • Не прочитано</p>
              </div>
          </div>
      </button>

      <!-- Item: Шаблоны протоколов ГЭК -->
      <article class="bg-surface-container-low opacity-75 border border-outline-variant border-dashed rounded-xl overflow-hidden cursor-not-allowed" aria-label="Шаблоны протоколов ГЭК, Требуется авторизация преподавателя">
          <div class="p-md flex items-center gap-md">
              <div class="w-12 h-12 rounded-lg bg-surface-variant flex items-center justify-center shrink-0" aria-hidden="true">
                  <span class="material-symbols-outlined text-outline text-2xl">lock</span>
              </div>
              <div class="flex-1">
                  <div class="flex justify-between items-center">
                      <h3 class="font-body-md text-body-md font-medium text-on-surface-variant">Шаблоны протоколов ГЭК</h3>
                      <span class="material-symbols-outlined text-outline text-sm" aria-hidden="true">lock</span>
                  </div>
                  <p class="text-on-surface-variant font-label-sm text-label-sm italic">DOCX • Доступ для преподавателей</p>
              </div>
          </div>
      </article>
  </section>

  <!-- Featured Achievement Card (Academic Style) -->
  <section class="mt-xl p-md rounded-xl bg-primary text-white relative overflow-hidden md:col-span-2 lg:col-span-3 md:mt-0" aria-labelledby="milestone-heading">
      <!-- Subtle Mesh Pattern -->
      <div class="absolute inset-0 opacity-10 bg-[radial-gradient(circle_at_top_right,_var(--tw-gradient-stops))] from-secondary via-transparent to-transparent" aria-hidden="true"></div>
      <div class="relative z-10 space-y-base">
          <h4 id="milestone-heading" class="font-title-lg text-title-lg">Обновление базы</h4>
          <p class="font-body-md text-body-md opacity-90">Добавлены новые методические рекомендации Роспотребнадзора по специальности «Инфекционные болезни».</p>
          <div class="flex justify-between items-center pt-sm">
              <div class="flex -space-x-2" aria-hidden="true">
                  <div class="w-8 h-8 rounded-full border-2 border-primary bg-surface bg-cover bg-center" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuDTkpGhASwhDeLenfRNMZcijTNElZqzlfS1QyTo8XPoXh1hJP6MSp4v60tetsZmS5G5FrwHsvikh8b3IbEpBn5doInts9X-Ybt_gcFZkxqwVkyDlDHHPBCvDykLsyvm9tt2E8fwSEuF1vZoHDV7G8bRrTyiykgIGDg394SgyI8x9nqw7dC8bBVy7sNwUrGehqoGXVLKlp2Idp5t8jdtMwaDd1kWYAW-5As2a0RyWmZBHIYb13m3pc9S9RbMpsjQegZ62QaX02sZXkC9')"></div>
                  <div class="w-8 h-8 rounded-full border-2 border-primary bg-surface flex items-center justify-center text-[0.625rem] font-bold text-primary">NEW</div>
              </div>
              <button class="bg-secondary text-on-secondary-fixed font-label-md text-label-md px-md py-xs rounded-full hover:brightness-110 active:scale-95 transition-all">Читать</button>
          </div>
      </div>
  </section>
</main>

<!-- Bottom Navigation Bar -->
<nav class="md:hidden fixed bottom-0 w-full z-50 bg-surface border-t border-outline-variant flex justify-around items-center px-sm pb-base pt-xs" aria-label="Bottom Navigation">
  <button class="flex flex-col items-center justify-center bg-primary-container text-on-primary-container rounded-full px-base py-xs active:scale-90 duration-200" aria-current="page">
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;" aria-hidden="true">library_books</span>
      <span class="font-label-sm text-label-sm">Материалы</span>
  </button>
  <button class="flex flex-col items-center justify-center text-on-surface-variant px-base py-xs hover:text-primary active:scale-90 duration-200 transition-all">
      <span class="material-symbols-outlined" aria-hidden="true">bookmark</span>
      <span class="font-label-sm text-label-sm">Избранное</span>
  </button>
  <button class="flex flex-col items-center justify-center text-on-surface-variant px-base py-xs hover:text-primary active:scale-90 duration-200 transition-all">
      <span class="material-symbols-outlined" aria-hidden="true">search</span>
      <span class="font-label-sm text-label-sm">Поиск</span>
  </button>
  <button class="flex flex-col items-center justify-center text-on-surface-variant px-base py-xs hover:text-primary active:scale-90 duration-200 transition-all">
      <span class="material-symbols-outlined" aria-hidden="true">person</span>
      <span class="font-label-sm text-label-sm">Профиль</span>
  </button>
</nav>

<!-- Contextual FAB -->
<button
  class="fixed bottom-24 right-md w-14 h-14 bg-secondary text-on-secondary-fixed rounded-full shadow-lg flex items-center justify-center hover:scale-105 active:scale-90 transition-all z-40"
  aria-label="Запросить актуализацию"
>
  <span class="material-symbols-outlined text-2xl" aria-hidden="true">edit_document</span>
</button>
