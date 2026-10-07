package de.pritcloud.appstow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int DEFAULT_STATISTICS_TOP_LIMIT = 10;
    private static final int STATISTICS_TOP_LIMIT_ALL = -1;

    private static final int REQUEST_CREATE_BACKUP = 1001;
    private static final int REQUEST_RESTORE_BACKUP = 1002;

    private static final String BACKUP_RUNTIME_PREFS =
            "backup_runtime";
    private static final String KEY_RESTORE_RESULT_PENDING =
            "restore_result_pending";
    private static final String KEY_RESTORE_RESULT_SUCCESS =
            "restore_result_success";
    private static final String KEY_RESTORE_RESULT_MESSAGE =
            "restore_result_message";
    private static final String ACTION_RESTORE_FINISHED =
            MainActivity.class.getName()
                    + ".action.RESTORE_FINISHED";

    private static final String STATE_PAGE = "main_page";
    private static final String STATE_APP_SEARCH = "app_search";
    private static final String STATE_OVERVIEW_SEARCH = "overview_search";
    private static final String STATE_GRID_OPEN_SECTION =
            "grid_open_section";

    private static final String OVERVIEW_DISPLAY_PREFS =
            "overview_display";
    private static final String STATISTICS_DISPLAY_PREFS =
            "statistics_display";
    private static final String KEY_STATISTICS_PERIOD =
            "statistics_period";
    private static final String KEY_STATISTICS_CARD_ORDER =
            "statistics_card_order_v2";
    private static final String KEY_STATISTICS_TOP_LIMIT =
            "statistics_top_limit";
    private static final String KEY_GRID_MODE =
            "grid_mode";
    private static final String KEY_GRID_COLUMNS =
            "grid_columns";
    private static final String KEY_SECTION_GRID_COLUMNS =
            "section_grid_columns_";
    private static final String STATE_APP_FILTER = "app_filter";
    private static final String STATE_EXPANDED_SECTION = "expanded_section";
    private static final String STATE_HELP_SCROLL = "help_scroll";
    private static final String STATE_ABOUT_SCROLL = "about_scroll";
    private static final String STATE_STATISTICS_PERIOD =
            "statistics_period";

    private static final String STATE_SHORTCUT_EDITOR =
            "shortcut_editor";
    private static final String STATE_CATEGORY_EDITOR =
            "category_editor";
    private static final String STATE_CATEGORY_ID =
            "category_editor_id";
    private static final String STATE_CATEGORY_NAME =
            "category_editor_name";
    private static final String STATE_CATEGORY_SYMBOL =
            "category_editor_symbol";
    private static final String STATE_EMOJI_PICKER =
            "emoji_picker";
    private static final String STATE_EMOJI_QUERY =
            "emoji_query";
    private static final String STATE_EMOJI_POSITION =
            "emoji_position";

    private static final String STATE_APP_ASSIGNMENT =
            "app_assignment";
    private static final String STATE_APP_ASSIGNMENT_PACKAGE =
            "app_assignment_package";
    private static final String STATE_APP_ASSIGNMENT_CATEGORIES =
            "app_assignment_categories";

    private static final String PAGE_OVERVIEW = "overview";
    private static final String PAGE_APPS = "apps";
    private static final String PAGE_CATEGORIES = "categories";
    private static final String PAGE_SHORTCUTS = "shortcuts";
    private static final String PAGE_STATISTICS = "statistics";
    private static final String PAGE_SORTING = "sorting";
    private static final String PAGE_BACKUP = "backup";
    private static final String PAGE_HELP = "help";
    private static final String PAGE_ABOUT = "about";

    private DrawerLayout drawerLayout;
    private View mainHeader;
    private View mainContentArea;
    private TextView pageTitle;
    private TextView pageMessage;
    private RecyclerView appList;
    private RecyclerView overviewList;
    private RecyclerView categoryList;
    private View categoryManagement;
    private TextView categoryEmptyMessage;
    private RecyclerView shortcutList;
    private View shortcutManagement;
    private TextView shortcutEmptyMessage;

    private View statisticsManagement;
    private TextView statisticsSummaryLabel;
    private RecyclerView statisticsList;
    private StatisticsAdapter statisticsAdapter;
    private ItemTouchHelper statisticsItemTouchHelper;
    private ImageButton statisticsSortButton;
    private ImageButton statisticsPeriodButton;
    private ImageButton statisticsLimitButton;
    private ImageButton statisticsResetButton;
    private boolean statisticsSortMode;
    private int statisticsTopLimit =
            DEFAULT_STATISTICS_TOP_LIMIT;
    private UsageStatisticsStore.Period statisticsPeriod =
            UsageStatisticsStore.Period.ONE_MONTH;

    private ViewStub sortingStub;
    private ScrollView sortingManagement;
    private RadioGroup sortingModeGroup;
    private View sortingManualSettings;
    private View sortingSemiSettings;
    private View sortingAutomaticSettings;
    private View sortingTimeSettings;
    private View sortingTimeDetails;
    private CheckBox sortingSemiFavorites;
    private CheckBox sortingSemiCategories;
    private CheckBox sortingSemiApps;
    private CheckBox sortingSemiShortcuts;
    private CheckBox sortingTimeProfileEnabled;
    private CheckBox sortingAlwaysStartFavorites;
    private CheckBox sortingSuggestionsEnabled;
    private TextView sortingSuggestionInterval;
    private TextView sortingSuggestionFavoriteCount;
    private TextView sortingSuggestionCheckNow;
    private TextView sortingDayStart;
    private TextView sortingEveningStart;
    private TextView sortingFavoriteCount;
    private SortingSettingsStore sortingSettingsStore;
    private boolean updatingSortingUi;
    private boolean sortingSuggestionAutomaticCheckPending;
    private boolean sortingSuggestionManualCheckPending;

    private View backupManagement;

    private ViewStub helpStub;
    private ScrollView helpManagement;
    private View helpShortcutSection;

    private ViewStub aboutStub;
    private ScrollView aboutManagement;
    private TextView aboutVersion;
    private TextView aboutPackage;
    private EditText appSearch;
    private String assignmentSearchQuery = "";
    private String overviewSearchQuery = "";
    private View appSearchContainer;
    private ImageButton appSearchClear;
    private ImageButton overviewLayoutButton;
    private ImageButton overviewSortButton;
    private GridLayoutManager overviewGridLayoutManager;
    private GridLayoutManager appGridLayoutManager;
    private GridLayoutManager categoryGridLayoutManager;
    private GridLayoutManager shortcutGridLayoutManager;
    private SharedPreferences overviewDisplayPreferences;
    private SharedPreferences statisticsDisplayPreferences;
    private int overviewGridColumns = 4;
    private ImageButton appFilterButton;
    private ImageButton shortcutHelpButton;
    private ImageButton topNavigationButton;
    private boolean showingOverview;
    private boolean showOnlyUnassignedApps;
    private boolean appsLoading;
    private boolean appsLoaded;
    private boolean appsReloadPending;
    private int packageChangeSequence;
    private boolean packageChangeSequenceInitialized;
    private int appContentGeneration;
    private String currentPage = PAGE_OVERVIEW;

    private final OnBackInvokedCallback pageBackCallback =
            this::showOverview;
    private boolean pageBackCallbackRegistered;

    private boolean restoreReceiverRegistered;
    private boolean packageReceiverRegistered;

    private final BroadcastReceiver packageChangeReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    String action =
                            intent.getAction();

                    if (!Intent.ACTION_PACKAGE_ADDED.equals(action)
                            && !Intent.ACTION_PACKAGE_REMOVED.equals(action)
                            && !Intent.ACTION_PACKAGE_REPLACED.equals(action)
                            && !Intent.ACTION_PACKAGE_CHANGED.equals(action)) {

                        return;
                    }

                    requestAppReload();

                    // Sequenz auf den aktuellen Stand setzen, damit
                    // onResume nicht unnötig denselben Reload wiederholt.
                    packageChangeSequenceInitialized = false;
                    initializePackageChangeSequence();
                }
            };

    private final BroadcastReceiver restoreReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (ACTION_RESTORE_FINISHED.equals(
                            intent.getAction())) {

                        handlePendingRestoreResult();
                    }
                }
            };

    private final ExecutorService appLoader =
            Executors.newSingleThreadExecutor();

    private final ExecutorService backupExecutor =
            Executors.newSingleThreadExecutor();

    private final ExecutorService emojiLoader =
            Executors.newSingleThreadExecutor();

    private final ExecutorService emojiSearchExecutor =
            Executors.newSingleThreadExecutor();

    private AppIconLoader appIconLoader;
    private AppAdapter appAdapter;
    private OverviewAdapter overviewAdapter;
    private FavoritesStore favoritesStore;
    private CategoryStore categoryStore;
    private CategoryAdapter categoryAdapter;

    private ShortcutStore shortcutStore;
    private ShortcutAdapter shortcutAdapter;
    private UsageStatisticsStore usageStatisticsStore;
    private OverviewOrderStore overviewOrderStore;
    private SectionItemOrderStore sectionItemOrderStore;
    private ItemTouchHelper overviewItemTouchHelper;

    private Bundle shortcutEditorDraft;
    private Bundle categoryEditorDraft;
    private Bundle emojiPickerDraft;
    private Bundle appAssignmentDraft;
    private TextView activeCategorySymbolInput;

    private final List<AppEntry> apps = new ArrayList<>();
    private final List<OverviewSection> overviewSections =
            new ArrayList<>();

    private final List<String> manualOverviewBaselineOrder =
            new ArrayList<>();

    private final Set<String>
            currentAutomaticFavoriteItemIds =
            new HashSet<>();

    private UsageStatisticsStore.TimeProfile
            lastAppliedSortingProfile;

    private final Handler sortingProfileHandler =
            new Handler(
                    Looper.getMainLooper());

    private final Runnable sortingProfileBoundaryRunnable =
            () -> {

                if (isFinishing()
                        || isDestroyed()) {

                    return;
                }

                refreshOverviewForSortingSettingsChange();
                scheduleNextSortingProfileBoundary();
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View contentRoot =
                findViewById(
                        android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(
                contentRoot,
                (view, insets) -> {

                    Insets safeInsets =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());

                    view.setPadding(
                            safeInsets.left,
                            safeInsets.top,
                            safeInsets.right,
                            safeInsets.bottom);

                    return insets;
                });

        drawerLayout = findViewById(R.id.drawerLayout);
        mainHeader = findViewById(R.id.mainHeader);
        mainContentArea = findViewById(R.id.mainContentArea);
        pageTitle = findViewById(R.id.pageTitle);
        pageMessage = findViewById(R.id.pageMessage);
        appList = findViewById(R.id.appList);
        overviewList = findViewById(R.id.overviewList);
        categoryList = findViewById(R.id.categoryList);
        categoryManagement = findViewById(R.id.categoryManagement);
        categoryEmptyMessage = findViewById(R.id.categoryEmptyMessage);
        shortcutList =
                findViewById(R.id.shortcutList);
        shortcutManagement =
                findViewById(R.id.shortcutManagement);
        shortcutEmptyMessage =
                findViewById(R.id.shortcutEmptyMessage);

        statisticsManagement =
                findViewById(R.id.statisticsManagement);

        statisticsSummaryLabel =
                findViewById(R.id.statisticsSummaryLabel);

        statisticsList =
                findViewById(R.id.statisticsList);

        sortingStub =
                findViewById(R.id.sortingStub);

        backupManagement =
                findViewById(R.id.backupManagement);

        helpStub =
                findViewById(R.id.helpStub);

        aboutStub =
                findViewById(R.id.aboutStub);


        appSearch = findViewById(R.id.appSearch);
        appSearchContainer = findViewById(R.id.appSearchContainer);
        appSearchClear = findViewById(R.id.appSearchClear);
        overviewLayoutButton =
                findViewById(R.id.buttonOverviewLayout);
        overviewSortButton =
                findViewById(R.id.buttonSortOverview);

        statisticsSortButton =
                findViewById(R.id.buttonSortStatistics);

        statisticsPeriodButton =
                findViewById(R.id.buttonStatisticsPeriod);

        statisticsLimitButton =
                findViewById(R.id.buttonStatisticsLimit);

        statisticsResetButton =
                findViewById(R.id.buttonStatisticsReset);

        appFilterButton =
                findViewById(R.id.buttonFilterApps);

        shortcutHelpButton =
                findViewById(R.id.buttonShortcutHelp);

        topNavigationButton =
                findViewById(R.id.buttonOpenMenu);

        favoritesStore = new FavoritesStore(this);
        categoryStore = new CategoryStore(this);
        shortcutStore = new ShortcutStore(this);
        usageStatisticsStore =
                new UsageStatisticsStore(this);

        sortingSettingsStore =
                new SortingSettingsStore(this);

        overviewOrderStore =
                new OverviewOrderStore(this);

        overviewDisplayPreferences =
                getSharedPreferences(
                        OVERVIEW_DISPLAY_PREFS,
                        MODE_PRIVATE);

        statisticsDisplayPreferences =
                getSharedPreferences(
                        STATISTICS_DISPLAY_PREFS,
                        MODE_PRIVATE);

        String savedStatisticsPeriod =
                statisticsDisplayPreferences.getString(
                        KEY_STATISTICS_PERIOD,
                        UsageStatisticsStore.Period
                                .ONE_MONTH
                                .name());

        try {
            statisticsPeriod =
                    UsageStatisticsStore.Period.valueOf(
                            savedStatisticsPeriod);
        } catch (IllegalArgumentException exception) {
            statisticsPeriod =
                    UsageStatisticsStore.Period.ONE_MONTH;
        }

        int savedStatisticsTopLimit =
                statisticsDisplayPreferences.getInt(
                        KEY_STATISTICS_TOP_LIMIT,
                        DEFAULT_STATISTICS_TOP_LIMIT);

        statisticsTopLimit =
                isValidStatisticsTopLimit(
                        savedStatisticsTopLimit)
                        ? savedStatisticsTopLimit
                        : DEFAULT_STATISTICS_TOP_LIMIT;

        overviewGridColumns = Math.max(
                3,
                Math.min(
                        5,
                        overviewDisplayPreferences.getInt(
                                KEY_GRID_COLUMNS,
                                4)));

        sectionItemOrderStore =
                new SectionItemOrderStore(this);

        statisticsAdapter =
                new StatisticsAdapter(
                        this::startStatisticsDrag);

        statisticsList.setLayoutManager(
                new LinearLayoutManager(this));

        statisticsList.setAdapter(
                statisticsAdapter);

        statisticsItemTouchHelper =
                new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(
                                ItemTouchHelper.UP
                                        | ItemTouchHelper.DOWN,
                                0) {

                            @Override
                            public boolean isLongPressDragEnabled() {
                                return false;
                            }

                            @Override
                            public int getMovementFlags(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder) {

                                return makeMovementFlags(
                                        statisticsSortMode
                                                ? ItemTouchHelper.UP
                                                | ItemTouchHelper.DOWN
                                                : 0,
                                        0);
                            }

                            @Override
                            public boolean onMove(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder source,
                                    RecyclerView.ViewHolder target) {

                                if (!statisticsSortMode) {
                                    return false;
                                }

                                return statisticsAdapter.moveCard(
                                        source.getBindingAdapterPosition(),
                                        target.getBindingAdapterPosition());
                            }

                            @Override
                            public void onSwiped(
                                    RecyclerView.ViewHolder viewHolder,
                                    int direction) {
                            }

                            @Override
                            public void clearView(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder) {

                                super.clearView(
                                        recyclerView,
                                        viewHolder);

                                if (statisticsSortMode) {
                                    saveStatisticsCardOrder();
                                }
                            }
                        });

        statisticsItemTouchHelper.attachToRecyclerView(
                statisticsList);

        categoryAdapter = new CategoryAdapter(
                new CategoryAdapter.Listener() {
                    @Override
                    public void onRename(CategoryEntry category) {
                        showRenameCategoryDialog(category);
                    }

                    @Override
                    public void onDelete(CategoryEntry category) {
                        showDeleteCategoryDialog(category);
                    }
                });

        categoryGridLayoutManager =
                new GridLayoutManager(
                        this,
                        1);

        categoryList.setLayoutManager(
                categoryGridLayoutManager);

        categoryList.setAdapter(
                categoryAdapter);

        findViewById(R.id.categoryAddButton)
                .setOnClickListener(v ->
                        showAddCategoryDialog());

        shortcutAdapter =
                new ShortcutAdapter(
                        categoryStore,
                        new ShortcutAdapter.Listener() {
                            @Override
                            public void onEdit(
                                    ShortcutEntry shortcut) {

                                showShortcutEditor(
                                        shortcut);
                            }

                            @Override
                            public void onDelete(
                                    ShortcutEntry shortcut) {

                                showDeleteShortcutDialog(
                                        shortcut);
                            }

                            @Override
                            public void onFavorite(
                                    ShortcutEntry shortcut) {

                                shortcutStore.toggleFavorite(
                                        shortcut.id);

                                refreshShortcuts();
                            }
                        });

        shortcutGridLayoutManager =
                new GridLayoutManager(
                        this,
                        1);

        shortcutList.setLayoutManager(
                shortcutGridLayoutManager);

        shortcutList.setAdapter(
                shortcutAdapter);

        findViewById(R.id.shortcutAddButton)
                .setOnClickListener(v ->
                        showShortcutEditor(null));

        findViewById(R.id.backupCreateButton)
                .setOnClickListener(v ->
                        createBackup());

        findViewById(R.id.backupRestoreButton)
                .setOnClickListener(v ->
                        selectBackupForRestore());

        appIconLoader =
                new AppIconLoader(
                        getPackageManager());

        appAdapter = new AppAdapter(
                favoritesStore,
                categoryStore,
                appIconLoader,
                this::handleAppLongClick);

        appAdapter.setGridMode(
                overviewDisplayPreferences.getBoolean(
                        KEY_GRID_MODE,
                        false));

        appGridLayoutManager =
                new GridLayoutManager(
                        this,
                        appAdapter.isGridMode()
                                ? overviewGridColumns
                                : 1);

        appList.setLayoutManager(
                appGridLayoutManager);

        appList.setAdapter(appAdapter);
        appList.setHasFixedSize(true);

        rebuildOverviewSections();

        if (savedInstanceState != null) {
            restoreExpandedSection(
                    savedInstanceState.getString(
                            STATE_EXPANDED_SECTION));
        }

        overviewAdapter =
                new OverviewAdapter(
                        overviewSections,
                        favoritesStore,
                        categoryStore,
                        appIconLoader,
                        shortcutStore,
                        sectionItemOrderStore,
                        this::launchApp,
                        this::launchShortcut,
                        this::showShortcutEditor,
                        this::handleAppLongClick,
                        this::startOverviewDrag,
                        this::openGridSection,
                        this::showOverview,
                        this::showSectionGridColumnsDialog);

        applyOverviewSorting();

        overviewAdapter.setGridMode(
                overviewDisplayPreferences.getBoolean(
                        KEY_GRID_MODE,
                        false));

        overviewGridLayoutManager =
                new GridLayoutManager(
                        this,
                        overviewGridColumns);

        overviewGridLayoutManager.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return overviewAdapter.isGridCell(position)
                                ? 1
                                : overviewGridLayoutManager.getSpanCount();
                    }
                });

        overviewList.setLayoutManager(
                overviewGridLayoutManager);
        overviewList.setAdapter(overviewAdapter);

        overviewItemTouchHelper =
                new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(
                                ItemTouchHelper.UP
                                        | ItemTouchHelper.DOWN,
                                0) {

                            @Override
                            public boolean isLongPressDragEnabled() {
                                return false;
                            }

                            @Override
                            public int getMovementFlags(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder) {

                                if ((overviewAdapter.isSortMode()
                                        && overviewAdapter.isGridOverview())
                                        || overviewAdapter.isGridItemSortMode()) {

                                    return makeMovementFlags(
                                            ItemTouchHelper.UP
                                                    | ItemTouchHelper.DOWN
                                                    | ItemTouchHelper.LEFT
                                                    | ItemTouchHelper.RIGHT,
                                            0);
                                }

                                return super.getMovementFlags(
                                        recyclerView,
                                        viewHolder);
                            }

                            @Override
                            public boolean onMove(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder source,
                                    RecyclerView.ViewHolder target) {

                                int fromPosition =
                                        source.getBindingAdapterPosition();

                                int toPosition =
                                        target.getBindingAdapterPosition();

                                if (overviewAdapter.isSortMode()) {
                                    return overviewAdapter.moveSection(
                                            fromPosition,
                                            toPosition);
                                }

                                if (overviewAdapter.isItemSortMode()) {
                                    return overviewAdapter.moveItem(
                                            fromPosition,
                                            toPosition);
                                }

                                return false;
                            }

                            @Override
                            public void onSwiped(
                                    RecyclerView.ViewHolder viewHolder,
                                    int direction) {
                            }

                            @Override
                            public void clearView(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder) {

                                super.clearView(
                                        recyclerView,
                                        viewHolder);

                                if (overviewAdapter.isSortMode()) {
                                    saveOverviewOrderPreservingAutomation();
                                } else if (
                                        overviewAdapter.isItemSortMode()) {

                                    overviewAdapter.saveItemOrder();
                                }
                            }
                        });

        overviewItemTouchHelper.attachToRecyclerView(
                overviewList);

        overviewSortButton.setOnClickListener(v ->
                setOverviewSortMode(
                        !overviewAdapter.isSortMode()));

        overviewLayoutButton.setOnClickListener(v ->
                setOverviewGridMode(
                        !overviewAdapter.isGridMode()));

        overviewLayoutButton.setOnLongClickListener(v -> {
            showOverviewColumnsDialog();
            return true;
        });

        appFilterButton.setOnClickListener(v -> {
            showOnlyUnassignedApps =
                    !showOnlyUnassignedApps;

            updateAppFilterButton();
            renderApps(
                    appSearch.getText().toString());
        });

        shortcutHelpButton.setOnClickListener(v ->
                showHelp(true));

        statisticsResetButton.setOnClickListener(v ->
                showStatisticsResetDialog());

        statisticsLimitButton.setOnClickListener(v ->
                showStatisticsLimitDialog());

        statisticsPeriodButton.setOnClickListener(v ->
                showStatisticsPeriodDialog());

        statisticsSortButton.setOnClickListener(v ->
                setStatisticsSortMode(
                        !statisticsSortMode));

        findViewById(R.id.navApps).setOnClickListener(v ->
                showApps());

        findViewById(R.id.navCategories).setOnClickListener(v ->
                showCategoryManagement());

        findViewById(R.id.navShortcuts)
                .setOnClickListener(v ->
                        showShortcutManagement());

        findViewById(R.id.navStatistics)
                .setOnClickListener(v ->
                        showStatisticsManagement());

        findViewById(R.id.navSorting)
                .setOnClickListener(v ->
                        showSortingManagement());

        findViewById(R.id.navBackup)
                .setOnClickListener(v ->
                        showBackupManagement());

        findViewById(R.id.navHelp)
                .setOnClickListener(v ->
                        showHelp(false));

        findViewById(R.id.navAbout)
                .setOnClickListener(v ->
                        showAbout());


        appSearchClear.setOnClickListener(v -> {
            appSearch.setText("");
            appSearch.requestFocus();
        });

        appSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                appSearchClear.setVisibility(
                        editable.length() > 0
                                ? View.VISIBLE
                                : View.GONE);

                String query = editable.toString();

                if (PAGE_OVERVIEW.equals(currentPage)) {
                    overviewSearchQuery = query;

                    if (!query.trim().isEmpty()) {
                        overviewAdapter.finishItemSortMode();
                        setOverviewSortMode(false);
                    }

                    overviewSortButton.setVisibility(
                            query.trim().isEmpty()
                                    && !isAutomaticSectionOrderEnabled()
                                    ? View.VISIBLE
                                    : View.GONE);

                    overviewLayoutButton.setVisibility(
                            query.trim().isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE);

                    overviewAdapter.setSearchQuery(query);
                } else if (PAGE_APPS.equals(currentPage)) {
                    assignmentSearchQuery = query;

                    if (appSearchContainer.getVisibility()
                            == View.VISIBLE) {
                        renderApps(query);
                    }
                }
            }
        });

        initializePackageChangeSequence();
        registerPackageChangeReceiver();

        if (savedInstanceState == null) {
            sortingSuggestionAutomaticCheckPending =
                    true;

            showOverview();
            applyStartupFavorites();

        } else {
            showOnlyUnassignedApps =
                    savedInstanceState.getBoolean(
                            STATE_APP_FILTER,
                            false);

            assignmentSearchQuery =
                    savedInstanceState.getString(
                            STATE_APP_SEARCH,
                            "");

            overviewSearchQuery =
                    savedInstanceState.getString(
                            STATE_OVERVIEW_SEARCH,
                            "");

            int statisticsPeriodIndex =
                    savedInstanceState.getInt(
                            STATE_STATISTICS_PERIOD,
                            statisticsPeriod.ordinal());

            UsageStatisticsStore.Period[] periods =
                    UsageStatisticsStore.Period.values();

            if (statisticsPeriodIndex >= 0
                    && statisticsPeriodIndex
                    < periods.length) {

                statisticsPeriod =
                        periods[
                                statisticsPeriodIndex];
            }

            restorePage(
                    savedInstanceState.getString(
                            STATE_PAGE,
                            PAGE_OVERVIEW));

            if (PAGE_OVERVIEW.equals(currentPage)
                    && overviewAdapter.isGridMode()) {

                String savedGridSection =
                        savedInstanceState.getString(
                                STATE_GRID_OPEN_SECTION);

                if (savedGridSection != null) {
                    for (OverviewSection section :
                            overviewSections) {

                        if (savedGridSection.equals(
                                section.id)) {

                            openGridSection(section);
                            break;
                        }
                    }
                }
            }

            int helpScroll =
                    savedInstanceState.getInt(
                            STATE_HELP_SCROLL,
                            0);

            int aboutScroll =
                    savedInstanceState.getInt(
                            STATE_ABOUT_SCROLL,
                            0);

            if (PAGE_HELP.equals(currentPage)) {
                helpManagement.post(() ->
                        helpManagement.scrollTo(
                                0,
                                helpScroll));
            } else if (PAGE_ABOUT.equals(currentPage)) {
                aboutManagement.post(() ->
                        aboutManagement.scrollTo(
                                0,
                                aboutScroll));
            }
        }

        if (savedInstanceState != null) {
            restoreEditorState(
                    savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshAppsIfPackagesChanged();
        refreshOverviewForActiveTimeProfile();
        scheduleNextSortingProfileBoundary();
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState) {

        outState.putString(
                STATE_PAGE,
                currentPage);

        outState.putString(
                STATE_APP_SEARCH,
                assignmentSearchQuery);

        outState.putString(
                STATE_OVERVIEW_SEARCH,
                overviewSearchQuery);

        outState.putString(
                STATE_GRID_OPEN_SECTION,
                overviewAdapter.getGridOpenSectionId());

        outState.putBoolean(
                STATE_APP_FILTER,
                showOnlyUnassignedApps);

        outState.putInt(
                STATE_STATISTICS_PERIOD,
                statisticsPeriod.ordinal());

        for (OverviewSection section :
                overviewSections) {

            if (section.expanded) {
                outState.putString(
                        STATE_EXPANDED_SECTION,
                        section.id);

                break;
            }
        }

        outState.putInt(
                STATE_HELP_SCROLL,
                helpManagement != null
                        ? helpManagement.getScrollY()
                        : 0);

        outState.putInt(
                STATE_ABOUT_SCROLL,
                aboutManagement != null
                        ? aboutManagement.getScrollY()
                        : 0);

        if (shortcutEditorDraft != null) {
            outState.putBundle(
                    STATE_SHORTCUT_EDITOR,
                    new Bundle(
                            shortcutEditorDraft));
        }

        if (categoryEditorDraft != null) {
            outState.putBundle(
                    STATE_CATEGORY_EDITOR,
                    new Bundle(
                            categoryEditorDraft));
        }

        if (emojiPickerDraft != null) {
            outState.putBundle(
                    STATE_EMOJI_PICKER,
                    new Bundle(
                            emojiPickerDraft));
        }

        if (appAssignmentDraft != null) {
            outState.putBundle(
                    STATE_APP_ASSIGNMENT,
                    new Bundle(
                            appAssignmentDraft));
        }

        super.onSaveInstanceState(
                outState);
    }

    private void restoreEditorState(
            Bundle savedInstanceState) {

        Bundle shortcutState =
                savedInstanceState.getBundle(
                        STATE_SHORTCUT_EDITOR);

        if (shortcutState != null) {

            String shortcutId =
                    ShortcutEditorDialog
                            .getExistingId(
                                    shortcutState);

            ShortcutEntry shortcut =
                    shortcutId == null
                            ? null
                            : findShortcutById(
                                    shortcutId);

            if (shortcutId == null
                    || shortcut != null) {

                showShortcutEditor(
                        shortcut,
                        shortcutState);
            }
        }

        Bundle categoryState =
                savedInstanceState.getBundle(
                        STATE_CATEGORY_EDITOR);

        boolean categoryRestored =
                false;

        if (categoryState != null) {

            String categoryId =
                    categoryState.getString(
                            STATE_CATEGORY_ID);

            CategoryEntry category =
                    categoryId == null
                            ? null
                            : findCategoryById(
                                    categoryId);

            if (categoryId == null
                    || category != null) {

                showCategoryEditor(
                        category,
                        categoryState);

                categoryRestored =
                        true;
            }
        }

        Bundle emojiState =
                savedInstanceState.getBundle(
                        STATE_EMOJI_PICKER);

        if (categoryRestored
                && emojiState != null
                && activeCategorySymbolInput
                        != null) {

            showEmojiPicker(
                    activeCategorySymbolInput,
                    emojiState);
        }

        Bundle assignmentState =
                savedInstanceState.getBundle(
                        STATE_APP_ASSIGNMENT);

        if (assignmentState != null) {

            String packageName =
                    assignmentState.getString(
                            STATE_APP_ASSIGNMENT_PACKAGE,
                            "");

            if (!packageName.isEmpty()) {
                showAppCategoryAssignment(
                        packageName,
                        assignmentState);
            }
        }
    }

    private ShortcutEntry findShortcutById(
            String id) {

        for (ShortcutEntry shortcut :
                shortcutStore.getShortcuts()) {

            if (shortcut.id.equals(id)) {
                return shortcut;
            }
        }

        return null;
    }

    private CategoryEntry findCategoryById(
            String id) {

        for (CategoryEntry category :
                categoryStore.getCategories()) {

            if (category.id.equals(id)) {
                return category;
            }
        }

        return null;
    }

    private void restorePage(
            String page) {

        if (PAGE_APPS.equals(page)) {
            showApps();
        } else if (PAGE_CATEGORIES.equals(page)) {
            showCategoryManagement();
        } else if (PAGE_SHORTCUTS.equals(page)) {
            showShortcutManagement();
        } else if (PAGE_STATISTICS.equals(page)) {
            showStatisticsManagement();
        } else if (PAGE_SORTING.equals(page)) {
            showSortingManagement();
        } else if (PAGE_BACKUP.equals(page)) {
            showBackupManagement();
        } else if (PAGE_HELP.equals(page)) {
            showHelp(false);
        } else if (PAGE_ABOUT.equals(page)) {
            showAbout();
        } else {
            showOverview();
        }
    }

    private void restoreExpandedSection(
            String sectionId) {

        if (sectionId == null) {
            return;
        }

        for (OverviewSection section :
                overviewSections) {

            section.expanded =
                    section.id.equals(
                            sectionId);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter =
                new IntentFilter(
                        ACTION_RESTORE_FINISHED);

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                    restoreReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(
                    restoreReceiver,
                    filter);
        }

        restoreReceiverRegistered = true;

        handlePendingRestoreResult();
    }

    @Override
    protected void onStop() {

        sortingProfileHandler.removeCallbacks(
                sortingProfileBoundaryRunnable);

        if (restoreReceiverRegistered) {
            try {
                unregisterReceiver(
                        restoreReceiver);
            } catch (IllegalArgumentException ignored) {
            }

            restoreReceiverRegistered = false;
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (packageReceiverRegistered) {
            try {
                unregisterReceiver(
                        packageChangeReceiver);
            } catch (IllegalArgumentException ignored) {
            }

            packageReceiverRegistered = false;
        }

        if (pageBackCallbackRegistered) {
            getOnBackInvokedDispatcher()
                    .unregisterOnBackInvokedCallback(
                            pageBackCallback);

            pageBackCallbackRegistered = false;
        }

        if (appIconLoader != null) {
            appIconLoader.shutdown();
        }

        appLoader.shutdownNow();
        backupExecutor.shutdown();
        emojiLoader.shutdownNow();
        emojiSearchExecutor.shutdownNow();
        super.onDestroy();
    }

    private void rebuildOverviewSections() {
        Map<String, Boolean> expandedStates =
                new HashMap<>();

        for (OverviewSection section : overviewSections) {
            expandedStates.put(
                    section.id,
                    section.expanded);
        }

        overviewSections.clear();

        String favoritesTitle =
                "⭐ "
                        + getString(
                                R.string.overview_favorites);

        OverviewSection favorites =
                new OverviewSection(
                        "favorites",
                        favoritesTitle,
                        getString(R.string.overview_favorites_empty),
                        getString(R.string.overview_favorites),
                        "⭐");

        favorites.expanded =
                expandedStates.getOrDefault(
                        favorites.id,
                        false);

        overviewSections.add(favorites);

        for (CategoryEntry category :
                categoryStore.getCategories()) {

            String categoryTitle =
                    category.symbol
                            + " "
                            + category.name;

            OverviewSection section =
                    new OverviewSection(
                            "category:" + category.id,
                            categoryTitle,
                            getString(
                                    R.string.overview_category_empty),
                            category.name,
                            category.symbol);

            section.expanded =
                    expandedStates.getOrDefault(
                            section.id,
                            false);

            overviewSections.add(section);
        }

        String shortcutsTitle =
                "⚡ "
                        + getString(
                                R.string.overview_shortcuts);

        OverviewSection shortcuts =
                new OverviewSection(
                        "shortcuts",
                        shortcutsTitle,
                        getString(R.string.overview_shortcuts_empty),
                        getString(R.string.overview_shortcuts),
                        "⚡");

        shortcuts.expanded =
                expandedStates.getOrDefault(
                        shortcuts.id,
                        false);

        overviewSections.add(shortcuts);

        applySavedOverviewOrder();

        captureManualOverviewBaseline();
        applyOverviewSorting();
    }

    private void applySavedOverviewOrder() {
        List<String> savedOrder =
                overviewOrderStore.getOrder();

        if (savedOrder.isEmpty()) {
            return;
        }

        List<OverviewSection> defaultSections =
                new ArrayList<>(overviewSections);

        Map<String, OverviewSection> remaining =
                new HashMap<>();

        for (OverviewSection section : defaultSections) {
            remaining.put(section.id, section);
        }

        List<OverviewSection> ordered =
                new ArrayList<>();

        for (String id : savedOrder) {
            OverviewSection section =
                    remaining.remove(id);

            if (section != null) {
                ordered.add(section);
            }
        }

        for (OverviewSection section : defaultSections) {
            if (!remaining.containsKey(section.id)) {
                continue;
            }

            remaining.remove(section.id);

            if ("favorites".equals(section.id)) {
                ordered.add(0, section);
                continue;
            }

            if ("shortcuts".equals(section.id)) {
                ordered.add(section);
                continue;
            }

            int shortcutsIndex = -1;

            for (int i = 0; i < ordered.size(); i++) {
                if ("shortcuts".equals(
                        ordered.get(i).id)) {
                    shortcutsIndex = i;
                    break;
                }
            }

            if (shortcutsIndex >= 0) {
                ordered.add(
                        shortcutsIndex,
                        section);
            } else {
                ordered.add(section);
            }
        }

        overviewSections.clear();
        overviewSections.addAll(ordered);
    }

    private void captureManualOverviewBaseline() {

        manualOverviewBaselineOrder.clear();

        for (OverviewSection section :
                overviewSections) {

            manualOverviewBaselineOrder.add(
                    section.id);
        }
    }

    private void applyOverviewSorting() {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        updateFavoriteEditingState();

        if (settings.mode
                == SortingSettingsStore.Mode.MANUAL) {

            currentAutomaticFavoriteItemIds.clear();
            lastAppliedSortingProfile = null;

            if (overviewAdapter != null) {

                overviewAdapter.setSemiAutomaticSorting(
                        false,
                        false,
                        false,
                        false,
                        false,
                        new HashMap<>(),
                        new HashMap<>(),
                        new HashMap<>(),
                        false);
            }

            return;
        }

        UsageStatisticsStore.TimeProfile activeProfile =
                settings.timeProfileEnabled
                        ? usageStatisticsStore
                                .getCurrentTimeProfile(
                                        settings.dayStartHour,
                                        settings.eveningStartHour)
                        : UsageStatisticsStore.TimeProfile.DAY;

        lastAppliedSortingProfile =
                settings.timeProfileEnabled
                        ? activeProfile
                        : null;

        UsageStatisticsStore.SortingSnapshot sorting =
                usageStatisticsStore.getSortingSnapshot(
                        activeProfile,
                        settings.dayStartHour,
                        settings.eveningStartHour);

        UsageStatisticsStore.Snapshot overall =
                sorting.getOverallWeighted();

        UsageStatisticsStore.Snapshot profile =
                sorting.getProfileWeighted();

        UsageStatisticsStore.Snapshot profileLaunches =
                sorting.getProfileLaunches();

        if (settings.mode
                == SortingSettingsStore.Mode.AUTOMATIC) {

            currentAutomaticFavoriteItemIds.clear();

            currentAutomaticFavoriteItemIds.addAll(
                    buildAutomaticFavoriteItemIds(
                            settings,
                            sorting));

            applyAutomaticSectionOrder(
                    sorting.getOverallSectionCounts(),
                    sorting.getProfileSectionCounts(),
                    sorting.getProfileSectionLaunches(),
                    settings.timeProfileEnabled);

            if (overviewAdapter != null) {

                overviewAdapter.setFullAutomaticSorting(
                        currentAutomaticFavoriteItemIds,
                        createItemScoreMap(
                                overall),
                        createItemScoreMap(
                                profile),
                        createItemScoreMap(
                                profileLaunches),
                        settings.timeProfileEnabled);
            }

            return;
        }

        currentAutomaticFavoriteItemIds.clear();

        if (settings.semiCategories) {

            applySemiAutomaticSectionOrder(
                    sorting.getOverallSectionCounts(),
                    sorting.getProfileSectionCounts(),
                    sorting.getProfileSectionLaunches(),
                    settings.timeProfileEnabled);
        }

        if (overviewAdapter != null) {

            overviewAdapter.setSemiAutomaticSorting(
                    true,
                    settings.semiFavorites,
                    settings.semiCategories,
                    settings.semiApps,
                    settings.semiShortcuts,
                    createItemScoreMap(
                            overall),
                    createItemScoreMap(
                            profile),
                    createItemScoreMap(
                            profileLaunches),
                    settings.timeProfileEnabled);
        }
    }

    private Map<String, Integer> createItemScoreMap(
            UsageStatisticsStore.Snapshot snapshot) {

        Map<String, Integer> result =
                new HashMap<>();

        for (Map.Entry<String, Integer> entry :
                snapshot.getAppCounts()
                        .entrySet()) {

            result.put(
                    SectionItemOrderStore.appItemId(
                            entry.getKey()),
                    entry.getValue());
        }

        for (Map.Entry<String, Integer> entry :
                snapshot.getShortcutCounts()
                        .entrySet()) {

            result.put(
                    SectionItemOrderStore.shortcutItemId(
                            entry.getKey()),
                    entry.getValue());
        }

        return result;
    }

    private Set<String> buildAutomaticFavoriteItemIds(
            SortingSettingsStore.Settings settings,
            UsageStatisticsStore.SortingSnapshot sorting) {

        List<String> allItemIds =
                new ArrayList<>();

        for (AppEntry app :
                apps) {

            allItemIds.add(
                    SectionItemOrderStore
                            .appItemId(
                                    app.packageName));
        }

        for (ShortcutEntry shortcut :
                shortcutStore.getShortcuts()) {

            allItemIds.add(
                    SectionItemOrderStore
                            .shortcutItemId(
                                    shortcut.id));
        }

        /*
         * Die bestehende Favoritenreihenfolge dient
         * weiterhin als stabile Basis für Gleichstände.
         * Neue Kandidaten werden anschließend angefügt.
         */
        List<String> baselineIds =
                sectionItemOrderStore
                        .getOrderedIds(
                                "favorites",
                                allItemIds);

        List<String> ranked =
                AutomaticSortingPlanner
                        .selectTopUsedIds(
                                baselineIds,
                                settings.automaticFavoriteCount,
                                createItemScoreMap(
                                        sorting.getOverallWeighted()),
                                createItemScoreMap(
                                        sorting.getProfileWeighted()),
                                createItemScoreMap(
                                        sorting.getProfileLaunches()),
                                settings.timeProfileEnabled);

        return new HashSet<>(
                ranked);
    }

    private void applyAutomaticSectionOrder(
            Map<String, Integer> overallSectionCounts,
            Map<String, Integer> profileSectionCounts,
            Map<String, Integer> profileSectionLaunches,
            boolean timeProfileEnabled) {

        Map<String, OverviewSection> sectionsById =
                new HashMap<>();

        for (OverviewSection section :
                overviewSections) {

            sectionsById.put(
                    section.id,
                    section);
        }

        List<String> ranked =
                AutomaticSortingPlanner
                        .rankSections(
                                manualOverviewBaselineOrder,
                                overallSectionCounts,
                                profileSectionCounts,
                                profileSectionLaunches,
                                timeProfileEnabled);

        List<OverviewSection> reordered =
                new ArrayList<>();

        for (String id :
                ranked) {

            OverviewSection section =
                    sectionsById.get(
                            id);

            if (section != null) {

                reordered.add(
                        section);
            }
        }

        if (reordered.size()
                == overviewSections.size()) {

            overviewSections.clear();

            overviewSections.addAll(
                    reordered);
        }
    }

    private void applySemiAutomaticSectionOrder(
            Map<String, Integer> overallSectionCounts,
            Map<String, Integer> profileSectionCounts,
            Map<String, Integer> profileSectionLaunches,
            boolean timeProfileEnabled) {

        Set<String> automaticSectionIds =
                new HashSet<>();

        Map<String, OverviewSection> sectionsById =
                new HashMap<>();

        for (OverviewSection section :
                overviewSections) {

            automaticSectionIds.add(
                    section.id);

            sectionsById.put(
                    section.id,
                    section);
        }

        List<String> ranked =
                AutomaticSortEngine.rankSelectedIds(
                        manualOverviewBaselineOrder,
                        automaticSectionIds,
                        overallSectionCounts,
                        profileSectionCounts,
                        profileSectionLaunches,
                        timeProfileEnabled);

        List<OverviewSection> reordered =
                new ArrayList<>();

        for (String id :
                ranked) {

            OverviewSection section =
                    sectionsById.get(
                            id);

            if (section != null) {
                reordered.add(
                        section);
            }
        }

        if (reordered.size()
                != overviewSections.size()) {

            return;
        }

        overviewSections.clear();
        overviewSections.addAll(
                reordered);

        /*
         * Halbautomatik ist für aktivierte Bereiche
         * die Wahrheit. Der sichtbare automatische
         * Stand wird deshalb zur neuen Basis.
         */
        overviewOrderStore.saveOrder(
                overviewSections);

        captureManualOverviewBaseline();
    }

    private void materializeFullAutomaticState() {

        if (overviewAdapter != null) {

            overviewAdapter
                    .materializeFullAutomaticOrders();
        }

        overviewOrderStore.saveOrder(
                overviewSections);

        Set<String> favoritePackages =
                new HashSet<>();

        Set<String> favoriteShortcutIds =
                new HashSet<>();

        for (String itemId :
                currentAutomaticFavoriteItemIds) {

            if (itemId.startsWith(
                    "app:")) {

                favoritePackages.add(
                        itemId.substring(
                                "app:".length()));

            } else if (itemId.startsWith(
                    "shortcut:")) {

                favoriteShortcutIds.add(
                        itemId.substring(
                                "shortcut:".length()));
            }
        }

        /*
         * Beim Verlassen der Vollautomatik wird die
         * sichtbare gemischte Favoritenbelegung exakt
         * als normaler manueller Zustand gespeichert.
         */
        favoritesStore.replaceAll(
                favoritePackages);

        shortcutStore.replaceFavorites(
                favoriteShortcutIds);

        captureManualOverviewBaseline();
    }

    private void updateFavoriteEditingState() {

        if (sortingSettingsStore == null) {
            return;
        }

        boolean enabled =
                sortingSettingsStore
                        .load()
                        .mode
                        != SortingSettingsStore.Mode.AUTOMATIC;

        if (appAdapter != null) {

            appAdapter.setFavoriteEditingEnabled(
                    enabled);
        }

        if (shortcutAdapter != null) {

            shortcutAdapter.setFavoriteEditingEnabled(
                    enabled);
        }
    }

    private boolean isCurrentFavoriteApp(
            String packageName) {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (settings.mode
                == SortingSettingsStore.Mode.AUTOMATIC) {

            return currentAutomaticFavoriteItemIds
                    .contains(
                            SectionItemOrderStore
                                    .appItemId(
                                            packageName));
        }

        return favoritesStore.isFavorite(
                packageName);
    }

    private boolean isCurrentFavoriteShortcut(
            String shortcutId) {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (settings.mode
                == SortingSettingsStore.Mode.AUTOMATIC) {

            return currentAutomaticFavoriteItemIds
                    .contains(
                            SectionItemOrderStore
                                    .shortcutItemId(
                                            shortcutId));
        }

        for (ShortcutEntry shortcut :
                shortcutStore.getShortcuts()) {

            if (shortcut.id.equals(
                    shortcutId)) {

                return shortcut.favorite;
            }
        }

        return false;
    }

    private void refreshOverviewForSortingSettingsChange() {

        if (overviewAdapter == null) {
            return;
        }

        rebuildOverviewSections();

        overviewAdapter.setApps(
                apps);
    }

    private void refreshOverviewForActiveTimeProfile() {

        if (sortingSettingsStore == null
                || usageStatisticsStore == null
                || overviewAdapter == null) {

            return;
        }

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (settings.mode
                == SortingSettingsStore.Mode.MANUAL
                || !settings.timeProfileEnabled) {

            lastAppliedSortingProfile = null;
            return;
        }

        UsageStatisticsStore.TimeProfile current =
                usageStatisticsStore
                        .getCurrentTimeProfile(
                                settings.dayStartHour,
                                settings.eveningStartHour);

        /*
         * Rückkehr aus einer gestarteten App soll
         * nicht sofort umsortieren. Nur ein echter
         * Profilwechsel löst hier neu aus.
         */
        if (current
                == lastAppliedSortingProfile) {

            return;
        }

        refreshOverviewForSortingSettingsChange();
    }

    private void scheduleNextSortingProfileBoundary() {

        sortingProfileHandler.removeCallbacks(
                sortingProfileBoundaryRunnable);

        if (sortingSettingsStore == null
                || usageStatisticsStore == null
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (settings.mode
                == SortingSettingsStore.Mode.MANUAL
                || !settings.timeProfileEnabled) {

            return;
        }

        long delay =
                usageStatisticsStore
                        .millisUntilNextTimeProfileBoundary(
                                settings.dayStartHour,
                                settings.eveningStartHour);

        sortingProfileHandler.postDelayed(
                sortingProfileBoundaryRunnable,
                delay + 1000L);
    }

    private void saveOverviewOrderPreservingAutomation() {

        overviewOrderStore.saveOrder(
                overviewSections);

        captureManualOverviewBaseline();
    }

    private void startOverviewDrag(
            RecyclerView.ViewHolder holder) {

        if (overviewItemTouchHelper != null
                && (overviewAdapter.isSortMode()
                || overviewAdapter.isItemSortMode())) {

            overviewItemTouchHelper.startDrag(
                    holder);
        }
    }

    private void setOverviewSortMode(
            boolean enabled) {

        if (enabled
                && isAutomaticSectionOrderEnabled()) {

            return;
        }

        if (!enabled
                && overviewAdapter.isSortMode()) {

            saveOverviewOrderPreservingAutomation();
        }

        overviewAdapter.setSortMode(enabled);

        overviewSortButton.setImageResource(
                enabled
                        ? R.drawable.ic_done
                        : R.drawable.ic_sort_overview);

        overviewSortButton.setContentDescription(
                getString(
                        enabled
                                ? R.string.action_finish_sorting
                                : R.string.action_sort_overview));
    }

    private void updateOverviewLayoutButton() {
        boolean grid = overviewAdapter.isGridMode();

        overviewLayoutButton.setImageResource(
                grid
                        ? R.drawable.ic_list_view
                        : R.drawable.ic_grid_view);

        overviewLayoutButton.setContentDescription(
                getString(
                        grid
                                ? R.string.action_show_list_view
                                : R.string.action_show_grid_view));
    }

    private void setOverviewGridMode(boolean enabled) {
        overviewAdapter.finishItemSortMode();
        setOverviewSortMode(false);

        overviewAdapter.setGridMode(enabled);

        overviewDisplayPreferences.edit()
                .putBoolean(KEY_GRID_MODE, enabled)
                .apply();

        updateOverviewLayoutButton();
        overviewList.scrollToPosition(0);
    }

    private void showOverviewColumnsDialog() {
        CharSequence[] choices = {
                getString(R.string.grid_columns_3),
                getString(R.string.grid_columns_4),
                getString(R.string.grid_columns_5)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.grid_columns_title)
                .setSingleChoiceItems(
                        choices,
                        overviewGridColumns - 3,
                        (dialog, selected) -> {
                            overviewGridColumns = selected + 3;

                            overviewDisplayPreferences.edit()
                                    .putInt(
                                            KEY_GRID_COLUMNS,
                                            overviewGridColumns)
                                    .apply();

                            overviewGridLayoutManager.setSpanCount(
                                    overviewGridColumns);

                            overviewList.scrollToPosition(0);
                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private int getSectionGridColumns(
            OverviewSection section) {

        int savedColumns =
                overviewDisplayPreferences.getInt(
                        KEY_SECTION_GRID_COLUMNS + section.id,
                        overviewGridColumns);

        return Math.max(
                3,
                Math.min(5, savedColumns));
    }

    private void showSectionGridColumnsDialog(
            OverviewSection section) {

        String settingKey =
                KEY_SECTION_GRID_COLUMNS + section.id;

        boolean hasCustomColumns =
                overviewDisplayPreferences.contains(
                        settingKey);

        int selectedChoice =
                hasCustomColumns
                        ? getSectionGridColumns(section) - 2
                        : 0;

        CharSequence[] choices = {
                getString(
                        R.string.grid_columns_global,
                        overviewGridColumns),
                getString(R.string.grid_columns_3),
                getString(R.string.grid_columns_4),
                getString(R.string.grid_columns_5)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.grid_columns_title)
                .setSingleChoiceItems(
                        choices,
                        selectedChoice,
                        (dialog, selected) -> {

                            if (selected == 0) {
                                overviewDisplayPreferences
                                        .edit()
                                        .remove(settingKey)
                                        .apply();
                            } else {
                                overviewDisplayPreferences
                                        .edit()
                                        .putInt(
                                                settingKey,
                                                selected + 2)
                                        .apply();
                            }

                            if (section.id.equals(
                                    overviewAdapter
                                            .getGridOpenSectionId())) {

                                overviewGridLayoutManager
                                        .setSpanCount(
                                                getSectionGridColumns(
                                                        section));

                                overviewList.scrollToPosition(0);
                            }

                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private void openGridSection(
            OverviewSection section) {

        setOverviewSortMode(false);

        overviewGridLayoutManager.setSpanCount(
                getSectionGridColumns(section));

        overviewAdapter.openGridSection(section);

        setTopNavigation(false);

        mainHeader.setVisibility(View.GONE);

        ViewGroup.MarginLayoutParams contentParams =
                (ViewGroup.MarginLayoutParams)
                        mainContentArea.getLayoutParams();

        contentParams.topMargin = 0;
        mainContentArea.setLayoutParams(contentParams);

        overviewLayoutButton.setVisibility(View.GONE);
        overviewSortButton.setVisibility(View.GONE);

        appSearchContainer.setVisibility(View.GONE);
        appSearch.clearFocus();

        pageTitle.setText(section.gridLabel);
        overviewList.scrollToPosition(0);
    }

    private void updateAppFilterButton() {
        appFilterButton.setImageResource(
                showOnlyUnassignedApps
                        ? R.drawable.ic_filter_apps_active
                        : R.drawable.ic_filter_apps);

        appFilterButton.setContentDescription(
                getString(
                        showOnlyUnassignedApps
                                ? R.string.action_show_all_apps
                                : R.string.action_filter_unassigned_apps));
    }

    private void hideOverviewSortMode() {
        overviewAdapter.finishItemSortMode();
        setOverviewSortMode(false);

        overviewLayoutButton.setVisibility(View.GONE);
        overviewSortButton.setVisibility(View.GONE);
        appFilterButton.setVisibility(View.GONE);
    }

    private void applyStartupFavorites() {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (!settings.alwaysStartFavorites) {

            return;
        }

        /*
         * Dieser Zugriff beeinflusst ausschließlich
         * die Sortierbewertung. Sichtbare Statistik-
         * Zähler und Gesamtstarts bleiben unverändert.
         */
        usageStatisticsStore.recordSectionAccess(
                "favorites");

        refreshOverviewForSortingSettingsChange();

        OverviewSection favorites =
                null;

        int favoritesIndex =
                -1;

        for (int i = 0;
             i < overviewSections.size();
             i++) {

            OverviewSection section =
                    overviewSections.get(i);

            if ("favorites".equals(
                    section.id)) {

                favorites =
                        section;

                favoritesIndex =
                        i;

                break;
            }
        }

        if (favorites == null) {
            return;
        }

        if (overviewAdapter.isGridMode()) {

            openGridSection(
                    favorites);

            return;
        }

        for (OverviewSection section :
                overviewSections) {

            section.expanded =
                    section == favorites;
        }

        overviewAdapter.setApps(
                apps);

        if (favoritesIndex >= 0) {

            overviewList.scrollToPosition(
                    favoritesIndex);
        }
    }

    private boolean isAutomaticSectionOrderEnabled() {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        return settings.mode
                == SortingSettingsStore.Mode.AUTOMATIC
                || (settings.mode
                == SortingSettingsStore.Mode.SEMI_AUTOMATIC
                && settings.semiCategories);
    }

    private void showOverview() {
        currentPage = PAGE_OVERVIEW;
        setTopNavigation(true);

        shortcutHelpButton.setVisibility(View.GONE);
        appFilterButton.setVisibility(View.GONE);

        categoryManagement.setVisibility(View.GONE);
        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        hideHelpPage();
        hideAboutPage();

        overviewAdapter.finishItemSortMode();
        setOverviewSortMode(false);

        overviewGridLayoutManager.setSpanCount(
                overviewGridColumns);

        overviewAdapter.setGridMode(
                overviewAdapter.isGridMode());

        updateOverviewLayoutButton();

        overviewLayoutButton.setVisibility(
                overviewSearchQuery.trim().isEmpty()
                        ? View.VISIBLE
                        : View.GONE);

        overviewSortButton.setVisibility(
                overviewSearchQuery.trim().isEmpty()
                        && !isAutomaticSectionOrderEnabled()
                        ? View.VISIBLE
                        : View.GONE);

        rebuildOverviewSections();
        overviewAdapter.setApps(apps);

        pageTitle.setText(R.string.nav_overview);

        appSearchContainer.setVisibility(View.VISIBLE);
        appSearch.setText(overviewSearchQuery);
        appSearchClear.setVisibility(
                appSearch.length() > 0
                        ? View.VISIBLE
                        : View.GONE);
        appSearch.clearFocus();

        appList.setVisibility(View.GONE);
        pageMessage.setVisibility(View.GONE);
        overviewList.setVisibility(View.VISIBLE);

        drawerLayout.closeDrawer(GravityCompat.END);

        loadAppsAsync();
    }

    private void showApps() {
        currentPage = PAGE_APPS;
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(View.GONE);

        hideOverviewSortMode();

        appFilterButton.setVisibility(View.VISIBLE);
        updateAppFilterButton();

        categoryManagement.setVisibility(View.GONE);
        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        hideHelpPage();
        hideAboutPage();
        overviewList.setVisibility(View.GONE);

        boolean grid =
                overviewAdapter.isGridMode();

        int columns =
                grid
                        ? overviewGridColumns
                        : 1;

        boolean layoutChanged =
                appAdapter.isGridMode() != grid
                        || appGridLayoutManager.getSpanCount()
                        != columns;

        appAdapter.setGridMode(grid);

        appGridLayoutManager.setSpanCount(
                columns);

        if (layoutChanged) {
            appList.scrollToPosition(0);
        }

        requestAppReload();

        pageTitle.setText(R.string.nav_apps);

        appSearchContainer.setVisibility(View.VISIBLE);
        appSearch.setText(assignmentSearchQuery);
        appSearchClear.setVisibility(
                appSearch.length() > 0
                        ? View.VISIBLE
                        : View.GONE);

        renderApps(appSearch.getText().toString());
        appList.post(
                appAdapter::refreshVisibleState);

        drawerLayout.closeDrawer(GravityCompat.END);
    }

    private void showCategoryManagement() {
        currentPage = PAGE_CATEGORIES;
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(View.GONE);

        hideOverviewSortMode();

        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        hideHelpPage();
        hideAboutPage();

        pageTitle.setText(R.string.nav_categories);

        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        overviewList.setVisibility(View.GONE);
        appList.setVisibility(View.GONE);
        pageMessage.setVisibility(View.GONE);

        categoryManagement.setVisibility(View.VISIBLE);

        boolean grid =
                overviewAdapter.isGridMode();

        int columns =
                grid
                        ? overviewGridColumns
                        : 1;

        boolean layoutChanged =
                categoryAdapter.isGridMode()
                        != grid
                        || categoryGridLayoutManager
                                .getSpanCount()
                        != columns;

        categoryAdapter.setGridMode(
                grid);

        categoryGridLayoutManager.setSpanCount(
                columns);

        if (layoutChanged) {
            categoryList.scrollToPosition(
                    0);
        }

        refreshCategories();

        drawerLayout.closeDrawer(GravityCompat.END);
    }

    private void showStatisticsManagement() {
        currentPage = PAGE_STATISTICS;
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(
                View.GONE);

        hideOverviewSortMode();

        categoryManagement.setVisibility(
                View.GONE);

        shortcutManagement.setVisibility(
                View.GONE);

        backupManagement.setVisibility(
                View.GONE);

        hideHelpPage();
        hideAboutPage();

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_statistics);

        statisticsResetButton.setVisibility(
                View.VISIBLE);

        statisticsLimitButton.setVisibility(
                View.VISIBLE);

        statisticsPeriodButton.setVisibility(
                View.VISIBLE);

        statisticsSortButton.setVisibility(
                View.VISIBLE);

        setStatisticsSortMode(false);
        updateStatisticsSummary();

        statisticsManagement.setVisibility(
                View.VISIBLE);

        loadAppsAsync();
        refreshStatistics();

        drawerLayout.closeDrawer(
                GravityCompat.END);
    }

    private int getStatisticsPeriodIndex() {
        if (statisticsPeriod
                == UsageStatisticsStore.Period.THREE_MONTHS) {

            return 1;
        }

        if (statisticsPeriod
                == UsageStatisticsStore.Period.SIX_MONTHS) {

            return 2;
        }

        if (statisticsPeriod
                == UsageStatisticsStore.Period.ONE_YEAR) {

            return 3;
        }

        return 0;
    }

    private String getStatisticsPeriodLabel() {
        if (statisticsPeriod
                == UsageStatisticsStore.Period.THREE_MONTHS) {

            return getString(
                    R.string.statistics_period_three_months);
        }

        if (statisticsPeriod
                == UsageStatisticsStore.Period.SIX_MONTHS) {

            return getString(
                    R.string.statistics_period_six_months);
        }

        if (statisticsPeriod
                == UsageStatisticsStore.Period.ONE_YEAR) {

            return getString(
                    R.string.statistics_period_one_year);
        }

        return getString(
                R.string.statistics_period_one_month);
    }

    private static boolean isValidStatisticsTopLimit(
            int value) {

        return value == 5
                || value == 10
                || value == 25
                || value == 50
                || value == STATISTICS_TOP_LIMIT_ALL;
    }

    private int getStatisticsLimitIndex() {
        if (statisticsTopLimit == 5) {
            return 0;
        }

        if (statisticsTopLimit == 10) {
            return 1;
        }

        if (statisticsTopLimit == 25) {
            return 2;
        }

        if (statisticsTopLimit == 50) {
            return 3;
        }

        return 4;
    }

    private void updateStatisticsSummary() {
        if (!appsLoaded
                && apps.isEmpty()) {

            statisticsSummaryLabel.setText(
                    R.string.statistics_summary_loading);

            return;
        }

        int appCount =
                apps.size();

        boolean allApps =
                statisticsTopLimit
                        == STATISTICS_TOP_LIMIT_ALL;

        int textResource;

        if (allApps) {
            if (statisticsPeriod
                    == UsageStatisticsStore.Period.THREE_MONTHS) {

                textResource =
                        R.string.statistics_summary_all_three_months;

            } else if (statisticsPeriod
                    == UsageStatisticsStore.Period.SIX_MONTHS) {

                textResource =
                        R.string.statistics_summary_all_six_months;

            } else if (statisticsPeriod
                    == UsageStatisticsStore.Period.ONE_YEAR) {

                textResource =
                        R.string.statistics_summary_all_one_year;

            } else {
                textResource =
                        R.string.statistics_summary_all_one_month;
            }

            statisticsSummaryLabel.setText(
                    getString(
                            textResource,
                            appCount));

            return;
        }

        if (statisticsPeriod
                == UsageStatisticsStore.Period.THREE_MONTHS) {

            textResource =
                    R.string.statistics_summary_top_three_months;

        } else if (statisticsPeriod
                == UsageStatisticsStore.Period.SIX_MONTHS) {

            textResource =
                    R.string.statistics_summary_top_six_months;

        } else if (statisticsPeriod
                == UsageStatisticsStore.Period.ONE_YEAR) {

            textResource =
                    R.string.statistics_summary_top_one_year;

        } else {
            textResource =
                    R.string.statistics_summary_top_one_month;
        }

        statisticsSummaryLabel.setText(
                getString(
                        textResource,
                        statisticsTopLimit,
                        appCount));
    }

    private void showStatisticsLimitDialog() {
        CharSequence[] choices = {
                getString(
                        R.string.statistics_limit_5),
                getString(
                        R.string.statistics_limit_10),
                getString(
                        R.string.statistics_limit_25),
                getString(
                        R.string.statistics_limit_50),
                getString(
                        R.string.statistics_limit_all)
        };

        int[] values = {
                5,
                10,
                25,
                50,
                STATISTICS_TOP_LIMIT_ALL
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.statistics_limit_title)
                .setSingleChoiceItems(
                        choices,
                        getStatisticsLimitIndex(),
                        (dialog, selected) -> {

                            statisticsTopLimit =
                                    values[selected];

                            statisticsDisplayPreferences
                                    .edit()
                                    .putInt(
                                            KEY_STATISTICS_TOP_LIMIT,
                                            statisticsTopLimit)
                                    .apply();

                            refreshStatistics();
                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private void showStatisticsPeriodDialog() {
        CharSequence[] choices = {
                getString(
                        R.string.statistics_period_one_month),
                getString(
                        R.string.statistics_period_three_months),
                getString(
                        R.string.statistics_period_six_months),
                getString(
                        R.string.statistics_period_one_year)
        };

        UsageStatisticsStore.Period[] periods = {
                UsageStatisticsStore.Period.ONE_MONTH,
                UsageStatisticsStore.Period.THREE_MONTHS,
                UsageStatisticsStore.Period.SIX_MONTHS,
                UsageStatisticsStore.Period.ONE_YEAR
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.statistics_period_title)
                .setSingleChoiceItems(
                        choices,
                        getStatisticsPeriodIndex(),
                        (dialog, selected) -> {

                            statisticsPeriod =
                                    periods[selected];

                            statisticsDisplayPreferences
                                    .edit()
                                    .putString(
                                            KEY_STATISTICS_PERIOD,
                                            statisticsPeriod.name())
                                    .apply();

                            updateStatisticsSummary();
                            refreshStatistics();
                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private void showStatisticsResetDialog() {
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.statistics_reset_title)
                        .setMessage(
                                R.string.statistics_reset_message)
                        .setPositiveButton(
                                R.string.statistics_reset_confirm,
                                (currentDialog, which) ->
                                        resetStatistics())
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(
                dialog,
                true);
    }

    private void resetStatistics() {
        usageStatisticsStore.clear();
        refreshStatistics();
    }

    private void startStatisticsDrag(
            RecyclerView.ViewHolder holder) {

        if (statisticsSortMode
                && statisticsItemTouchHelper != null) {

            statisticsItemTouchHelper.startDrag(
                    holder);
        }
    }

    private void setStatisticsSortMode(
            boolean enabled) {

        if (!enabled
                && statisticsSortMode) {

            saveStatisticsCardOrder();
        }

        statisticsSortMode = enabled;

        if (statisticsAdapter != null) {
            statisticsAdapter.setSortMode(
                    enabled);
        }

        if (statisticsSortButton != null) {
            statisticsSortButton.setImageResource(
                    enabled
                            ? R.drawable.ic_done
                            : R.drawable.ic_sort_overview);

            statisticsSortButton.setContentDescription(
                    getString(
                            enabled
                                    ? R.string.action_finish_statistics_sorting
                                    : R.string.action_sort_statistics));
        }
    }

    private List<String> getStatisticsCardOrder() {
        String saved =
                statisticsDisplayPreferences.getString(
                        KEY_STATISTICS_CARD_ORDER,
                        "");

        List<String> result =
                new ArrayList<>();

        if (saved == null
                || saved.isBlank()) {

            return result;
        }

        for (String id :
                saved.split(",")) {

            String normalized =
                    id.trim();

            if (!normalized.isEmpty()
                    && !result.contains(
                            normalized)) {

                result.add(
                        normalized);
            }
        }

        return result;
    }

    private void saveStatisticsCardOrder() {
        if (statisticsAdapter == null) {
            return;
        }

        statisticsDisplayPreferences
                .edit()
                .putString(
                        KEY_STATISTICS_CARD_ORDER,
                        String.join(
                                ",",
                                statisticsAdapter.getOrder()))
                .apply();
    }

    private List<StatisticsAdapter.Card>
            applyStatisticsCardOrder(
                    List<StatisticsAdapter.Card> cards,
                    List<String> order) {

        Map<String, StatisticsAdapter.Card> remaining =
                new HashMap<>();

        for (StatisticsAdapter.Card card :
                cards) {

            remaining.put(
                    card.id,
                    card);
        }

        List<StatisticsAdapter.Card> ordered =
                new ArrayList<>();

        for (String id :
                order) {

            StatisticsAdapter.Card card =
                    remaining.remove(
                            id);

            if (card != null) {
                ordered.add(
                        card);
            }
        }

        for (StatisticsAdapter.Card card :
                cards) {

            if (remaining.remove(
                    card.id) != null) {

                ordered.add(
                        card);
            }
        }

        return ordered;
    }

    private void refreshStatistics() {
        UsageStatisticsStore.Snapshot snapshot =
                usageStatisticsStore.getSnapshot(
                        statisticsPeriod);

        Map<String, String> appLabels =
                new HashMap<>();

        Set<String> favoriteApps =
                new HashSet<>();

        for (AppEntry app : apps) {
            appLabels.put(
                    app.packageName,
                    app.label);

            if (isCurrentFavoriteApp(
                    app.packageName)) {

                favoriteApps.add(
                        app.packageName);
            }
        }

        Map<String, String> categoryLabels =
                new HashMap<>();

        for (CategoryEntry category :
                categoryStore.getCategories()) {

            categoryLabels.put(
                    category.id,
                    category.name);
        }

        Map<String, String> shortcutLabels =
                new HashMap<>();

        for (ShortcutEntry shortcut :
                shortcutStore.getShortcuts()) {

            shortcutLabels.put(
                    shortcut.id,
                    shortcut.name);
        }

        Map<String, Integer> combinedShortcutCounts =
                new HashMap<>();

        Map<String, String> combinedShortcutLabels =
                new HashMap<>();

        Set<String> combinedFavoriteEntries =
                new HashSet<>();

        for (Map.Entry<String, Integer> entry :
                snapshot.getAppCounts().entrySet()) {

            String key =
                    "app:" + entry.getKey();

            combinedShortcutCounts.put(
                    key,
                    entry.getValue());

            String label =
                    appLabels.get(
                            entry.getKey());

            if (label != null) {
                combinedShortcutLabels.put(
                        key,
                        label);
            }

            if (favoriteApps.contains(
                    entry.getKey())) {

                combinedFavoriteEntries.add(
                        key);
            }
        }

        for (Map.Entry<String, Integer> entry :
                snapshot.getShortcutCounts().entrySet()) {

            String key =
                    "custom:" + entry.getKey();

            combinedShortcutCounts.put(
                    key,
                    entry.getValue());

            String label =
                    shortcutLabels.get(
                            entry.getKey());

            if (label != null) {
                combinedShortcutLabels.put(
                        key,
                        label);
            }

            if (isCurrentFavoriteShortcut(
                    entry.getKey())) {

                combinedFavoriteEntries.add(
                        key);
            }
        }

        List<StatisticsAdapter.Card> cards =
                new ArrayList<>();

        cards.add(
                new StatisticsAdapter.Card(
                        StatisticsAdapter.CARD_TOP_SHORTCUTS,
                        getString(
                                R.string.statistics_top_shortcuts_title),
                        buildStatisticsRows(
                                combinedShortcutCounts,
                                combinedShortcutLabels,
                                combinedFavoriteEntries,
                                true,
                                R.string.statistics_no_shortcut_launches),
                        null));

        cards.add(
                new StatisticsAdapter.Card(
                        StatisticsAdapter.CARD_TOP_CATEGORIES,
                        getString(
                                R.string.statistics_top_categories_title),
                        buildStatisticsRows(
                                snapshot.getCategoryCounts(),
                                categoryLabels,
                                null,
                                false,
                                R.string.statistics_no_category_launches),
                        null));

        cards.add(
                new StatisticsAdapter.Card(
                        StatisticsAdapter.CARD_TOP_APPS,
                        getString(
                                R.string.statistics_apps_title),
                        buildStatisticsRows(
                                snapshot.getAppCounts(),
                                appLabels,
                                favoriteApps,
                                true,
                                R.string.statistics_no_app_launches),
                        null));

        cards.add(
                new StatisticsAdapter.Card(
                        StatisticsAdapter.CARD_CUSTOM_SHORTCUTS,
                        getString(
                                R.string.statistics_custom_shortcuts_title),
                        buildStatisticsRows(
                                snapshot.getShortcutCounts(),
                                shortcutLabels,
                                null,
                                false,
                                R.string.statistics_no_custom_shortcut_launches),
                        null));

        cards.add(
                new StatisticsAdapter.Card(
                        StatisticsAdapter.CARD_UNUSED_APPS,
                        getString(
                                R.string.statistics_unused_apps_short_title),
                        buildUnusedAppRows(
                                snapshot.getAppCounts()),
                        null));

        cards.add(
                new StatisticsAdapter.Card(
                        StatisticsAdapter.CARD_TOTAL,
                        getString(
                                R.string.statistics_total_title),
                        new ArrayList<>(),
                        String.valueOf(
                                snapshot.getTotalLaunches())));

        List<String> order =
                statisticsAdapter.getOrder();

        if (order.isEmpty()) {
            order =
                    getStatisticsCardOrder();
        }

        statisticsAdapter.setCards(
                applyStatisticsCardOrder(
                        cards,
                        order));

        updateStatisticsSummary();
    }

    private List<StatisticsAdapter.Row>
            buildStatisticsRows(
                    Map<String, Integer> counts,
                    Map<String, String> labels,
                    Set<String> favorites,
                    boolean showFavoriteSlot,
                    int emptyTextResource) {

        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry :
                counts.entrySet()) {

            String label =
                    labels.get(
                            entry.getKey());

            if (label == null
                    || entry.getValue() == null
                    || entry.getValue() <= 0) {

                continue;
            }

            entries.add(
                    entry);
        }

        entries.sort(
                (first, second) -> {

                    int countComparison =
                            Integer.compare(
                                    second.getValue(),
                                    first.getValue());

                    if (countComparison != 0) {
                        return countComparison;
                    }

                    return labels.get(
                                    first.getKey())
                            .compareToIgnoreCase(
                                    labels.get(
                                            second.getKey()));
                });

        List<StatisticsAdapter.Row> rows =
                new ArrayList<>();

        if (entries.isEmpty()) {
            rows.add(
                    StatisticsAdapter.Row.message(
                            getString(
                                    emptyTextResource)));

            return rows;
        }

        int limit =
                statisticsTopLimit
                        == STATISTICS_TOP_LIMIT_ALL
                        ? entries.size()
                        : Math.min(
                                statisticsTopLimit,
                                entries.size());

        for (int i = 0;
             i < limit;
             i++) {

            Map.Entry<String, Integer> entry =
                    entries.get(i);

            boolean favorite =
                    favorites != null
                            && favorites.contains(
                                    entry.getKey());

            rows.add(
                    new StatisticsAdapter.Row(
                            labels.get(
                                    entry.getKey()),
                            String.valueOf(
                                    entry.getValue()),
                            showFavoriteSlot,
                            favorite));
        }

        return rows;
    }

    private List<StatisticsAdapter.Row>
            buildUnusedAppRows(
                    Map<String, Integer> appCounts) {

        List<StatisticsAdapter.Row> rows =
                new ArrayList<>();

        if (!appsLoaded) {
            rows.add(
                    StatisticsAdapter.Row.message(
                            getString(
                                    appsLoading
                                            ? R.string.statistics_apps_loading
                                            : R.string.statistics_apps_unavailable)));

            return rows;
        }

        List<AppEntry> unusedApps =
                new ArrayList<>();

        for (AppEntry app : apps) {
            if (appCounts.getOrDefault(
                    app.packageName,
                    0) > 0) {

                continue;
            }

            unusedApps.add(
                    app);
        }

        unusedApps.sort(
                (first, second) ->
                        first.label.compareToIgnoreCase(
                                second.label));

        if (unusedApps.isEmpty()) {
            rows.add(
                    StatisticsAdapter.Row.message(
                            getString(
                                    R.string.statistics_all_apps_used)));

            return rows;
        }

        for (AppEntry app :
                unusedApps) {

            rows.add(
                    new StatisticsAdapter.Row(
                            app.label,
                            null,
                            true,
                            isCurrentFavoriteApp(
                                    app.packageName)));
        }

        return rows;
    }

    private List<CategoryEntry> getCategoriesInOverviewOrder() {
        List<CategoryEntry> categories =
                categoryStore.getCategories();

        List<String> savedOrder =
                new ArrayList<>();

        if (sortingSettingsStore.load().mode
                == SortingSettingsStore.Mode.AUTOMATIC) {

            for (OverviewSection section :
                    overviewSections) {

                savedOrder.add(
                        section.id);
            }

        } else {

            savedOrder.addAll(
                    overviewOrderStore.getOrder());
        }

        if (savedOrder.isEmpty()) {
            return categories;
        }

        Map<String, CategoryEntry> remaining =
                new HashMap<>();

        for (CategoryEntry category : categories) {
            remaining.put(
                    "category:" + category.id,
                    category);
        }

        List<CategoryEntry> ordered =
                new ArrayList<>();

        for (String sectionId : savedOrder) {
            CategoryEntry category =
                    remaining.remove(sectionId);

            if (category != null) {
                ordered.add(category);
            }
        }

        for (CategoryEntry category : categories) {
            String sectionId =
                    "category:" + category.id;

            if (remaining.remove(sectionId) != null) {
                ordered.add(category);
            }
        }

        return ordered;
    }

    private void showShortcutManagement() {
        currentPage = PAGE_SHORTCUTS;
        setTopNavigation(false);

        hideOverviewSortMode();

        shortcutHelpButton.setVisibility(
                View.VISIBLE);

        categoryManagement.setVisibility(
                View.GONE);

        backupManagement.setVisibility(
                View.GONE);

        hideHelpPage();

        hideAboutPage();

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_shortcuts_manage);

        shortcutManagement.setVisibility(
                View.VISIBLE);

        boolean grid =
                overviewAdapter.isGridMode();

        int columns =
                grid
                        ? overviewGridColumns
                        : 1;

        boolean layoutChanged =
                shortcutAdapter.isGridMode()
                        != grid
                        || shortcutGridLayoutManager
                                .getSpanCount()
                        != columns;

        shortcutAdapter.setGridMode(
                grid);

        shortcutGridLayoutManager.setSpanCount(
                columns);

        if (layoutChanged) {

            shortcutList.scrollToPosition(
                    0);
        }

        refreshShortcuts();

        shortcutList.post(
                shortcutAdapter::refreshVisibleState);

        drawerLayout.closeDrawer(
                GravityCompat.END);
    }

    private void refreshShortcuts() {
        List<ShortcutEntry> shortcuts =
                shortcutStore.getShortcuts();

        shortcutAdapter.setShortcuts(
                shortcuts);

        shortcutEmptyMessage.setVisibility(
                shortcuts.isEmpty()
                        ? View.VISIBLE
                        : View.GONE);

        shortcutList.setVisibility(
                shortcuts.isEmpty()
                        ? View.GONE
                        : View.VISIBLE);
    }

    private void showShortcutEditor(
            ShortcutEntry shortcut) {

        showShortcutEditor(
                shortcut,
                null);
    }

    private void showShortcutEditor(
            ShortcutEntry shortcut,
            Bundle restoredState) {

        ShortcutEditorDialog.show(
                this,
                categoryStore,
                shortcut,
                restoredState,
                (name,
                 type,
                 target,
                 categoryIds,
                 favorite) -> {

                    if (shortcut == null) {
                        shortcutStore.add(
                                name,
                                type,
                                target,
                                categoryIds,
                                favorite);
                    } else {
                        shortcutStore.update(
                                shortcut.id,
                                name,
                                type,
                                target,
                                categoryIds,
                                favorite);
                    }

                    refreshShortcuts();

                    rebuildOverviewSections();

                    overviewAdapter.setApps(
                            apps);
                },
                new ShortcutEditorDialog.DraftListener() {
                    @Override
                    public void onDraftChanged(
                            Bundle draft) {

                        shortcutEditorDraft =
                                new Bundle(
                                        draft);
                    }

                    @Override
                    public void onClosed() {

                        shortcutEditorDraft =
                                null;
                    }
                });
    }

    private void showDeleteShortcutDialog(
            ShortcutEntry shortcut) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.shortcut_delete_title)
                        .setMessage(
                                getString(
                                        R.string.shortcut_delete_message,
                                        shortcut.name))
                        .setPositiveButton(
                                R.string.action_delete_shortcut,
                                (currentDialog, which) -> {
                                    shortcutStore.delete(
                                            shortcut.id);

                                    refreshShortcuts();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(
                dialog,
                true);
    }

    private void refreshCategories() {
        List<CategoryEntry> categories =
                getCategoriesInOverviewOrder();

        categoryAdapter.setCategories(categories);

        categoryEmptyMessage.setVisibility(
                categories.isEmpty()
                        ? View.VISIBLE
                        : View.GONE);

        categoryList.setVisibility(
                categories.isEmpty()
                        ? View.GONE
                        : View.VISIBLE);
    }

    private void showAddCategoryDialog() {

        showCategoryEditor(
                null,
                null);
    }

    private void showRenameCategoryDialog(
            CategoryEntry category) {

        showCategoryEditor(
                category,
                null);
    }

    private void showCategoryEditor(
            CategoryEntry category,
            Bundle restoredState) {

        String categoryId =
                category != null
                        ? category.id
                        : null;

        String initialName =
                category != null
                        ? category.name
                        : "";

        String initialSymbol =
                category != null
                        ? category.symbol
                        : CategoryStore.DEFAULT_SYMBOL;

        if (restoredState != null) {

            initialName =
                    restoredState.getString(
                            STATE_CATEGORY_NAME,
                            initialName);

            initialSymbol =
                    restoredState.getString(
                            STATE_CATEGORY_SYMBOL,
                            initialSymbol);
        }

        View editor =
                createCategoryEditor(
                        initialName,
                        initialSymbol);

        TextView symbolInput =
                editor.findViewById(
                        R.id.categorySymbolInput);

        EditText nameInput =
                editor.findViewById(
                        R.id.categoryNameInput);

        activeCategorySymbolInput =
                symbolInput;

        captureCategoryEditorDraft(
                categoryId,
                nameInput,
                symbolInput);

        nameInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        captureCategoryEditorDraft(
                                categoryId,
                                nameInput,
                                symbolInput);
                    }
                });

        symbolInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        captureCategoryEditorDraft(
                                categoryId,
                                nameInput,
                                symbolInput);
                    }
                });

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                category == null
                                        ? R.string.category_add_title
                                        : R.string.category_rename_title)
                        .setView(editor)
                        .setPositiveButton(
                                R.string.action_save,
                                (currentDialog,
                                 which) -> {

                                    boolean saved;

                                    if (categoryId
                                            == null) {

                                        saved =
                                                categoryStore
                                                        .addCategory(
                                                                nameInput
                                                                        .getText()
                                                                        .toString(),
                                                                symbolInput
                                                                        .getText()
                                                                        .toString());

                                    } else {

                                        saved =
                                                categoryStore
                                                        .renameCategory(
                                                                categoryId,
                                                                nameInput
                                                                        .getText()
                                                                        .toString(),
                                                                symbolInput
                                                                        .getText()
                                                                        .toString());
                                    }

                                    if (!saved) {
                                        Toast.makeText(
                                                this,
                                                R.string.category_invalid_name,
                                                Toast.LENGTH_SHORT)
                                                .show();
                                    }

                                    refreshCategories();

                                    rebuildOverviewSections();

                                    overviewAdapter.setApps(
                                            apps);
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        dialog.setOnDismissListener(
                ignored -> {

                    categoryEditorDraft =
                            null;

                    emojiPickerDraft =
                            null;

                    activeCategorySymbolInput =
                            null;
                });

        dialog.show();

        styleCategoryDialog(
                dialog,
                false);
    }

    private void captureCategoryEditorDraft(
            String categoryId,
            EditText nameInput,
            TextView symbolInput) {

        Bundle draft =
                new Bundle();

        if (categoryId != null) {
            draft.putString(
                    STATE_CATEGORY_ID,
                    categoryId);
        }

        draft.putString(
                STATE_CATEGORY_NAME,
                nameInput.getText()
                        .toString());

        draft.putString(
                STATE_CATEGORY_SYMBOL,
                symbolInput.getText()
                        .toString());

        categoryEditorDraft =
                draft;
    }

    private void showDeleteCategoryDialog(
            CategoryEntry category) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.category_delete_title)
                        .setMessage(
                                getString(
                                        R.string.category_delete_message,
                                        category.name))
                        .setPositiveButton(
                                R.string.action_delete_category,
                                (currentDialog, which) -> {
                                    categoryStore.deleteCategory(
                                            category.id);

                                    shortcutStore.removeCategory(
                                            category.id);

                                    sectionItemOrderStore.removeOrder(
                                            "category:" + category.id);

                                    overviewDisplayPreferences
                                            .edit()
                                            .remove(
                                                    KEY_SECTION_GRID_COLUMNS
                                                            + "category:"
                                                            + category.id)
                                            .apply();

                                    refreshCategories();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(dialog, true);
    }

    private void styleCategoryDialog(
            AlertDialog dialog,
            boolean destructive) {

        int neutralColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_text_primary);

        int dangerColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_danger);

        dialog.getButton(
                        AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(neutralColor);

        dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE)
                .setTextColor(
                        destructive
                                ? dangerColor
                                : neutralColor);
    }

    private void showEmojiPicker(
            TextView symbolInput) {

        showEmojiPicker(
                symbolInput,
                null);
    }

    private void showEmojiPicker(
            TextView symbolInput,
            Bundle restoredState) {

        Bundle draft =
                restoredState == null
                        ? new Bundle()
                        : new Bundle(
                                restoredState);

        String restoredQuery =
                draft.getString(
                        STATE_EMOJI_QUERY,
                        "");

        int restoredPosition =
                draft.getInt(
                        STATE_EMOJI_POSITION,
                        RecyclerView.NO_POSITION);

        draft.putString(
                STATE_EMOJI_QUERY,
                restoredQuery);

        draft.putInt(
                STATE_EMOJI_POSITION,
                restoredPosition);

        emojiPickerDraft =
                new Bundle(
                        draft);

        View pickerContent =
                getLayoutInflater()
                        .inflate(
                                R.layout.dialog_emoji_picker,
                                null);

        pickerContent.setFocusableInTouchMode(
                true);

        EditText searchInput =
                pickerContent.findViewById(
                        R.id.emojiSearchInput);

        ImageButton searchClear =
                pickerContent.findViewById(
                        R.id.emojiSearchClear);

        View categoryStrip =
                pickerContent.findViewById(
                        R.id.emojiCategoryStrip);

        LinearLayout categoryBar =
                pickerContent.findViewById(
                        R.id.emojiCategoryBar);

        RecyclerView emojiGrid =
                pickerContent.findViewById(
                        R.id.emojiGrid);

        View loading =
                pickerContent.findViewById(
                        R.id.emojiGridLoading);

        TextView searchEmpty =
                pickerContent.findViewById(
                        R.id.emojiSearchEmpty);

        searchInput.clearFocus();

        pickerContent.requestFocus();

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.category_emoji_picker_title)
                        .setView(
                                pickerContent)
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        EmojiSearchAdapter adapter =
                new EmojiSearchAdapter(
                        emoji -> {
                            symbolInput.setText(
                                    emoji);

                            dialog.dismiss();
                        });

        GridLayoutManager gridLayoutManager =
                new GridLayoutManager(
                        this,
                        8);

        emojiGrid.setLayoutManager(
                gridLayoutManager);

        emojiGrid.setAdapter(
                adapter);

        emojiGrid.setHasFixedSize(
                true);

        emojiGrid.setItemAnimator(
                null);

        emojiGrid.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(
                            RecyclerView recyclerView,
                            int dx,
                            int dy) {

                        int position =
                                gridLayoutManager
                                        .findFirstVisibleItemPosition();

                        if (position
                                != RecyclerView.NO_POSITION) {

                            draft.putInt(
                                    STATE_EMOJI_POSITION,
                                    position);

                            emojiPickerDraft =
                                    new Bundle(
                                            draft);
                        }
                    }
                });

        final Runnable[] pendingSearch =
                new Runnable[1];

        final int[] searchGeneration =
                new int[] {0};

        searchClear.setOnClickListener(
                ignored -> {
                    searchInput.setText(
                            "");

                    searchInput.requestFocus();
                });

        searchInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        String query =
                                editable.toString()
                                        .trim();

                        draft.putString(
                                STATE_EMOJI_QUERY,
                                editable.toString());

                        emojiPickerDraft =
                                new Bundle(
                                        draft);

                        int generation =
                                ++searchGeneration[0];

                        if (pendingSearch[0]
                                != null) {

                            searchInput.removeCallbacks(
                                    pendingSearch[0]);

                            pendingSearch[0] =
                                    null;
                        }

                        searchClear.setVisibility(
                                query.isEmpty()
                                        ? View.GONE
                                        : View.VISIBLE);

                        categoryStrip.setVisibility(
                                query.isEmpty()
                                        ? View.VISIBLE
                                        : View.GONE);

                        searchEmpty.setVisibility(
                                View.GONE);

                        if (query.isEmpty()) {

                            loadAllEmoji(
                                    adapter,
                                    emojiGrid,
                                    gridLayoutManager,
                                    categoryStrip,
                                    categoryBar,
                                    loading,
                                    searchEmpty,
                                    searchInput,
                                    dialog);

                            return;
                        }

                        loading.setVisibility(
                                View.GONE);

                        Runnable searchTask =
                                () ->
                                        emojiSearchExecutor.execute(
                                                () -> {

                                                    List<EmojiSearchIndex.Result>
                                                            matches =
                                                            EmojiSearchIndex.search(
                                                                    getApplicationContext(),
                                                                    query);

                                                    runOnUiThread(
                                                            () -> {

                                                                if (!dialog.isShowing()
                                                                        || generation
                                                                        != searchGeneration[0]
                                                                        || !query.equals(
                                                                                searchInput
                                                                                        .getText()
                                                                                        .toString()
                                                                                        .trim())) {

                                                                    return;
                                                                }

                                                                adapter.submitList(
                                                                        matches);

                                                                if (matches.isEmpty()) {

                                                                    emojiGrid.setVisibility(
                                                                            View.GONE);

                                                                    searchEmpty.setVisibility(
                                                                            View.VISIBLE);

                                                                } else {

                                                                    emojiGrid.setVisibility(
                                                                            View.VISIBLE);

                                                                    searchEmpty.setVisibility(
                                                                            View.GONE);
                                                                }
                                                            });
                                                });

                        pendingSearch[0] =
                                searchTask;

                        searchInput.postDelayed(
                                searchTask,
                                80L);
                    }
                });

        dialog.setOnShowListener(
                ignored -> {

                    searchInput.clearFocus();

                    pickerContent.requestFocus();

                    if (dialog.getWindow()
                            != null) {

                        dialog.getWindow()
                                .setSoftInputMode(
                                        android.view.WindowManager
                                                .LayoutParams
                                                .SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    }

                    if (searchInput.getText()
                            .toString()
                            .trim()
                            .isEmpty()) {

                        loadAllEmoji(
                                adapter,
                                emojiGrid,
                                gridLayoutManager,
                                categoryStrip,
                                categoryBar,
                                loading,
                                searchEmpty,
                                searchInput,
                                dialog);
                    }
                });

        dialog.setOnDismissListener(
                ignored -> {

                    ++searchGeneration[0];

                    if (pendingSearch[0]
                            != null) {

                        searchInput.removeCallbacks(
                                pendingSearch[0]);

                        pendingSearch[0] =
                                null;
                    }

                    emojiPickerDraft =
                            null;
                });

        if (!restoredQuery.isEmpty()) {
            searchInput.setText(
                    restoredQuery);

            searchInput.setSelection(
                    searchInput.length());
        }

        dialog.show();

        if (restoredPosition
                != RecyclerView.NO_POSITION) {

            restoreEmojiPositionWhenReady(
                    dialog,
                    emojiGrid,
                    gridLayoutManager,
                    restoredPosition,
                    0);
        }
    }

    private void restoreEmojiPositionWhenReady(
            AlertDialog dialog,
            RecyclerView emojiGrid,
            GridLayoutManager layoutManager,
            int position,
            int attempt) {

        if (!dialog.isShowing()) {
            return;
        }

        RecyclerView.Adapter<?> adapter =
                emojiGrid.getAdapter();

        int itemCount =
                adapter != null
                        ? adapter.getItemCount()
                        : 0;

        if (itemCount > 0) {

            int safePosition =
                    Math.max(
                            0,
                            Math.min(
                                    position,
                                    itemCount - 1));

            layoutManager
                    .scrollToPositionWithOffset(
                            safePosition,
                            0);

            return;
        }

        if (attempt >= 40) {
            return;
        }

        emojiGrid.postDelayed(
                () ->
                        restoreEmojiPositionWhenReady(
                                dialog,
                                emojiGrid,
                                layoutManager,
                                position,
                                attempt + 1),
                50L);
    }

    private void loadAllEmoji(
            EmojiSearchAdapter adapter,
            RecyclerView emojiGrid,
            GridLayoutManager gridLayoutManager,
            View categoryStrip,
            LinearLayout categoryBar,
            View loading,
            TextView searchEmpty,
            EditText searchInput,
            AlertDialog dialog) {

        loading.setVisibility(
                View.VISIBLE);

        searchEmpty.setVisibility(
                View.GONE);

        categoryStrip.setVisibility(
                View.VISIBLE);

        emojiLoader.execute(
                () -> {

                    List<EmojiSearchIndex.Result>
                            allEmoji =
                            EmojiSearchIndex.all(
                                    MainActivity.this);

                    List<EmojiSearchIndex.Category>
                            categories =
                            EmojiSearchIndex.categories(
                                    MainActivity.this);

                    runOnUiThread(
                            () -> {

                                if (!dialog.isShowing()
                                        || !searchInput
                                                .getText()
                                                .toString()
                                                .trim()
                                                .isEmpty()) {

                                    return;
                                }

                                loading.setVisibility(
                                        View.GONE);

                                if (categoryBar.getChildCount()
                                        == 0) {

                                    bindEmojiCategories(
                                            categoryBar,
                                            emojiGrid,
                                            gridLayoutManager,
                                            categories);
                                }

                                adapter.submitList(
                                        allEmoji);

                                if (allEmoji.isEmpty()) {
                                    emojiGrid.setVisibility(
                                            View.GONE);

                                    searchEmpty.setVisibility(
                                            View.VISIBLE);

                                } else {
                                    emojiGrid.setVisibility(
                                            View.VISIBLE);

                                    searchEmpty.setVisibility(
                                            View.GONE);
                                }
                            });
                });
    }

    private void bindEmojiCategories(
            LinearLayout categoryBar,
            RecyclerView emojiGrid,
            GridLayoutManager gridLayoutManager,
            List<EmojiSearchIndex.Category> categories) {

        categoryBar.removeAllViews();

        for (EmojiSearchIndex.Category category
                : categories) {

            TextView button =
                    (TextView)
                            getLayoutInflater()
                                    .inflate(
                                            R.layout.item_emoji_category,
                                            categoryBar,
                                            false);

            button.setText(
                    category.icon);

            button.setContentDescription(
                    category.label);

            button.setOnClickListener(
                    ignored -> {

                        emojiGrid.stopScroll();

                        gridLayoutManager
                                .scrollToPositionWithOffset(
                                        category.position,
                                        0);

                        for (int i = 0;
                             i < categoryBar.getChildCount();
                             i++) {

                            categoryBar.getChildAt(i)
                                    .setAlpha(
                                            categoryBar.getChildAt(i)
                                                    == button
                                                    ? 1f
                                                    : 0.55f);
                        }
                    });

            categoryBar.addView(
                    button);
        }

        if (categoryBar.getChildCount() > 0) {
            categoryBar.getChildAt(0)
                    .setAlpha(
                            1f);

            for (int i = 1;
                 i < categoryBar.getChildCount();
                 i++) {

                categoryBar.getChildAt(i)
                        .setAlpha(
                                0.55f);
            }
        }
    }

    private View createCategoryEditor(
            String name,
            String symbol) {

        ViewGroup root =
                findViewById(
                        android.R.id.content);

        View editor =
                getLayoutInflater()
                        .inflate(
                                R.layout.dialog_category_edit,
                                root,
                                false);

        TextView symbolInput =
                editor.findViewById(
                        R.id.categorySymbolInput);

        EditText nameInput =
                editor.findViewById(
                        R.id.categoryNameInput);

        styleCategoryInput(
                symbolInput);

        styleCategoryInput(
                nameInput);

        symbolInput.setText(
                symbol);

        symbolInput.setOnClickListener(v ->
                showEmojiPicker(
                        symbolInput));

        nameInput.setText(
                name);

        nameInput.setSelectAllOnFocus(
                true);

        return editor;
    }

    private void styleCategoryInput(
            TextView input) {

        int textColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_text_primary);

        int hintColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_text_secondary);

        int borderColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_border);

        input.setTextColor(
                textColor);

        input.setHintTextColor(
                hintColor);

        input.setBackgroundTintList(
                ColorStateList.valueOf(
                        borderColor));

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.Q) {

            input.setTextCursorDrawable(
                    R.drawable.search_cursor);
        }
    }

    private void registerPackageChangeReceiver() {
        if (packageReceiverRegistered) {
            return;
        }

        IntentFilter filter =
                new IntentFilter();

        filter.addAction(
                Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(
                Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(
                Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(
                Intent.ACTION_PACKAGE_CHANGED);

        filter.addDataScheme(
                "package");

        try {
            ContextCompat.registerReceiver(
                    this,
                    packageChangeReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED);

            packageReceiverRegistered = true;

        } catch (RuntimeException ignored) {
            // getChangedPackages() bleibt als Fallback aktiv.
        }
    }

    private void initializePackageChangeSequence() {
        if (packageChangeSequenceInitialized) {
            return;
        }

        try {
            ChangedPackages changedPackages =
                    getPackageManager()
                            .getChangedPackages(0);

            if (changedPackages != null) {
                packageChangeSequence =
                        changedPackages
                                .getSequenceNumber();
            }
        } catch (RuntimeException ignored) {
            packageChangeSequence = 0;
        }

        packageChangeSequenceInitialized = true;
    }

    private void refreshAppsIfPackagesChanged() {
        if (!packageChangeSequenceInitialized) {
            initializePackageChangeSequence();
            return;
        }

        ChangedPackages changedPackages;

        try {
            changedPackages =
                    getPackageManager()
                            .getChangedPackages(
                                    packageChangeSequence);
        } catch (RuntimeException ignored) {
            return;
        }

        if (changedPackages == null) {
            return;
        }

        packageChangeSequence =
                changedPackages
                        .getSequenceNumber();

        if (changedPackages
                .getPackageNames()
                .isEmpty()) {

            return;
        }

        requestAppReload();
    }

    private void requestAppReload() {
        appsLoaded = false;

        if (appsLoading) {
            appsReloadPending = true;
            return;
        }

        loadAppsAsync();
    }

    private void loadAppsAsync() {
        if (appsLoading
                || appsLoaded) {

            return;
        }

        appsLoading = true;
        appsReloadPending = false;

        int contentGeneration =
                ++appContentGeneration;

        PackageManager packageManager =
                getPackageManager();

        String ownPackageName =
                getPackageName();

        appLoader.execute(() -> {
            List<AppEntry> loadedApps =
                    new ArrayList<>();

            boolean loadSucceeded;

            try {
                loadedApps =
                        queryLauncherApps(
                                packageManager,
                                ownPackageName,
                                contentGeneration);

                loadSucceeded = true;

            } catch (RuntimeException exception) {
                loadSucceeded = false;
            }

            List<AppEntry> result =
                    loadedApps;

            boolean success =
                    loadSucceeded;

            runOnUiThread(() -> {
                if (isFinishing()
                        || isDestroyed()) {

                    return;
                }

                appsLoading = false;

                boolean reloadAgain =
                        appsReloadPending;

                appsReloadPending = false;

                if (success) {
                    appsLoaded = true;

                    apps.clear();
                    apps.addAll(result);

                    rebuildOverviewSections();
                    overviewAdapter.setApps(apps);
                }

                if (PAGE_APPS.equals(currentPage)) {
                    renderApps(
                            appSearch.getText()
                                    .toString());
                }

                if (PAGE_STATISTICS.equals(
                        currentPage)) {

                    refreshStatistics();
                }

                if (reloadAgain) {

                    appsLoaded = false;
                    loadAppsAsync();

                } else if (success) {

                    runPendingSortingSuggestionChecks();
                }
            });
        });
    }

    private List<AppEntry> queryLauncherApps(
            PackageManager packageManager,
            String ownPackageName,
            int contentGeneration) {

        Intent launcherIntent =
                new Intent(Intent.ACTION_MAIN);

        launcherIntent.addCategory(
                Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos;

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            resolveInfos =
                    packageManager.queryIntentActivities(
                            launcherIntent,
                            PackageManager.ResolveInfoFlags.of(0));
        } else {
            resolveInfos =
                    packageManager.queryIntentActivities(
                            launcherIntent,
                            0);
        }

        List<AppEntry> loadedApps =
                new ArrayList<>();

        Set<String> seenPackages =
                new HashSet<>();

        for (ResolveInfo resolveInfo :
                resolveInfos) {

            if (resolveInfo == null
                    || resolveInfo.activityInfo == null) {

                continue;
            }

            String packageName =
                    resolveInfo.activityInfo.packageName;

            String activityName =
                    resolveInfo.activityInfo.name;

            if (packageName == null
                    || packageName.isEmpty()
                    || activityName == null
                    || activityName.isEmpty()
                    || packageName.equals(
                            ownPackageName)
                    || !seenPackages.add(
                            packageName)) {

                continue;
            }

            CharSequence labelSequence;

            try {
                labelSequence =
                        resolveInfo.loadLabel(
                                packageManager);
            } catch (RuntimeException exception) {
                labelSequence = null;
            }

            String label =
                    labelSequence != null
                            ? labelSequence.toString()
                            : packageName;

            loadedApps.add(
                    new AppEntry(
                            label,
                            packageName,
                            resolveInfo,
                            null,
                            contentGeneration));
        }

        loadedApps.sort(
                (first, second) ->
                        first.label.compareToIgnoreCase(
                                second.label));

        return loadedApps;
    }

    private List<AppEntry> getFavoriteApps() {
        List<AppEntry> favoriteApps =
                new ArrayList<>();

        for (AppEntry app : apps) {
            if (favoritesStore.isFavorite(app.packageName)) {
                favoriteApps.add(app);
            }
        }

        return favoriteApps;
    }

    private void renderApps(String query) {
        String normalizedQuery =
                query.trim().toLowerCase(Locale.ROOT);

        List<AppEntry> filteredApps =
                new ArrayList<>();

        for (AppEntry app : apps) {
            if (!normalizedQuery.isEmpty()
                    && !app.searchLabel.contains(
                            normalizedQuery)
                    && !app.searchPackageName.contains(
                            normalizedQuery)) {
                continue;
            }

            if (showOnlyUnassignedApps
                    && categoryStore.hasAssignments(
                            app.packageName)) {
                continue;
            }

            filteredApps.add(app);
        }

        appAdapter.submitList(
                filteredApps);

        if (filteredApps.isEmpty()) {
            appList.setVisibility(View.GONE);
            pageMessage.setVisibility(View.VISIBLE);

            if (appsLoading
                    && !appsLoaded) {

                pageMessage.setText(
                        R.string.apps_loading);

            } else if (showOnlyUnassignedApps
                    && normalizedQuery.isEmpty()) {

                pageMessage.setText(
                        R.string.apps_unassigned_empty);

            } else {
                pageMessage.setText(
                        normalizedQuery.isEmpty()
                                ? R.string.apps_empty
                                : R.string.apps_search_empty);
            }
        } else {
            pageMessage.setVisibility(View.GONE);
            appList.setVisibility(View.VISIBLE);
        }
    }

    private void handleAppLongClick(
            AppEntry app) {

        showAppCategoryAssignment(
                app.packageName,
                null);
    }

    private void showAppCategoryAssignment(
            String packageName,
            Bundle restoredState) {

        List<CategoryEntry> categories =
                categoryStore.getCategories();

        if (categories.isEmpty()) {

            appAssignmentDraft =
                    null;

            Toast.makeText(
                    this,
                    R.string.category_assign_none,
                    Toast.LENGTH_LONG)
                    .show();

            return;
        }

        Set<String> selectedIds =
                new HashSet<>();

        if (restoredState != null) {

            ArrayList<String> restoredIds =
                    restoredState
                            .getStringArrayList(
                                    STATE_APP_ASSIGNMENT_CATEGORIES);

            if (restoredIds != null) {
                selectedIds.addAll(
                        restoredIds);
            }

        } else {

            selectedIds.addAll(
                    categoryStore
                            .getAssignedCategoryIds(
                                    packageName));
        }

        Set<String> validCategoryIds =
                new HashSet<>();

        for (CategoryEntry category :
                categories) {

            validCategoryIds.add(
                    category.id);
        }

        selectedIds.retainAll(
                validCategoryIds);

        Bundle draft =
                new Bundle();

        draft.putString(
                STATE_APP_ASSIGNMENT_PACKAGE,
                packageName);

        draft.putStringArrayList(
                STATE_APP_ASSIGNMENT_CATEGORIES,
                new ArrayList<>(
                        selectedIds));

        appAssignmentDraft =
                new Bundle(
                        draft);

        CharSequence[] names =
                new CharSequence[
                        categories.size()];

        boolean[] checked =
                new boolean[
                        categories.size()];

        for (int i = 0;
             i < categories.size();
             i++) {

            CategoryEntry category =
                    categories.get(i);

            names[i] =
                    category.name;

            checked[i] =
                    selectedIds.contains(
                            category.id);
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.category_assign_title)
                        .setMultiChoiceItems(
                                names,
                                checked,
                                (currentDialog,
                                 which,
                                 isChecked) -> {

                                    String categoryId =
                                            categories
                                                    .get(which)
                                                    .id;

                                    if (isChecked) {
                                        selectedIds.add(
                                                categoryId);
                                    } else {
                                        selectedIds.remove(
                                                categoryId);
                                    }

                                    draft.putStringArrayList(
                                            STATE_APP_ASSIGNMENT_CATEGORIES,
                                            new ArrayList<>(
                                                    selectedIds));

                                    appAssignmentDraft =
                                            new Bundle(
                                                    draft);
                                })
                        .setPositiveButton(
                                R.string.action_save,
                                (currentDialog,
                                 which) -> {

                                    categoryStore
                                            .setAssignedCategoryIds(
                                                    packageName,
                                                    selectedIds);

                                    if (appSearchContainer
                                            .getVisibility()
                                            == View.VISIBLE) {

                                        renderApps(
                                                appSearch
                                                        .getText()
                                                        .toString());

                                        appAdapter
                                                .refreshPackage(
                                                        packageName);
                                    }

                                    rebuildOverviewSections();

                                    overviewAdapter.setApps(
                                            apps);
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        dialog.setOnDismissListener(
                ignored ->
                        appAssignmentDraft =
                                null);

        dialog.show();

        styleCategoryDialog(
                dialog,
                false);
    }

    private String getStatisticsCategoryId(
            OverviewSection section) {

        if (section == null
                || !section.id.startsWith(
                        "category:")) {

            return null;
        }

        return section.id.substring(
                "category:".length());
    }

    private String getStatisticsSectionId(
            OverviewSection section) {

        if (section == null) {
            return null;
        }

        if ("favorites".equals(
                section.id)
                || "shortcuts".equals(
                section.id)
                || section.id.startsWith(
                "category:")) {

            return section.id;
        }

        return null;
    }

    private Set<String> getSortingSectionIds(
            OverviewSection sourceSection,
            Set<String> assignedCategoryIds) {

        Set<String> result =
                new HashSet<>();

        String sourceSectionId =
                getStatisticsSectionId(
                        sourceSection);

        if (sourceSectionId != null
                && !sourceSectionId.isBlank()) {

            result.add(
                    sourceSectionId);
        }

        if (assignedCategoryIds != null) {

            for (String categoryId :
                    assignedCategoryIds) {

                if (categoryId == null
                        || categoryId.isBlank()) {

                    continue;
                }

                result.add(
                        "category:"
                                + categoryId);
            }
        }

        return result;
    }

    private void recordAppLaunch(
            OverviewSection section,
            AppEntry app) {

        usageStatisticsStore.recordAppLaunch(
                app.packageName,
                getStatisticsCategoryId(
                        section),
                getSortingSectionIds(
                        section,
                        categoryStore
                                .getAssignedCategoryIds(
                                        app.packageName)));
    }

    private void recordShortcutLaunch(
            OverviewSection section,
            ShortcutEntry shortcut) {

        usageStatisticsStore.recordShortcutLaunch(
                shortcut.id,
                getStatisticsCategoryId(
                        section),
                getSortingSectionIds(
                        section,
                        shortcut.categoryIds));
    }

    private void launchShortcut(
            OverviewSection section,
            ShortcutEntry shortcut) {

        try {
            if (ShortcutEntry.TYPE_WEBSITE.equals(
                    shortcut.type)) {

                launchWebShortcut(
                        shortcut.target);

                recordShortcutLaunch(
                        section,
                        shortcut);

                return;
            }

            if (ShortcutEntry.TYPE_WEB_APP.equals(
                    shortcut.type)) {

                if (launchWebAppShortcut(
                        shortcut.target)) {

                    recordShortcutLaunch(
                            section,
                            shortcut);
                }

                return;
            }

            if (ShortcutEntry.TYPE_APP_SETTINGS.equals(
                    shortcut.type)) {

                launchAppSettings(
                        shortcut.target);

                recordShortcutLaunch(
                        section,
                        shortcut);

                return;
            }

            String target =
                    shortcut.target.trim();

            if (target.startsWith(
                    "package:")) {

                launchAppSettings(
                        target);

                recordShortcutLaunch(
                        section,
                        shortcut);

                return;
            }

            Intent intent;

            if (target.startsWith(
                    "intent:")) {

                intent =
                        Intent.parseUri(
                                target,
                                Intent.URI_INTENT_SCHEME);

            } else {
                intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(target));
            }

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);

            recordShortcutLaunch(
                    section,
                    shortcut);

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    R.string.shortcut_launch_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean launchWebAppShortcut(
            String target) {

        if (!NetworkAccess.hasUsableNetwork(
                this)) {

            showWebAppNetworkDialog();
            return false;
        }

        Intent intent =
                new Intent(
                        this,
                        WebAppActivity.class);

        intent.putExtra(
                WebAppActivity.EXTRA_URL,
                target);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(
                intent);

        return true;
    }

    private void showWebAppNetworkDialog() {

        NetworkAccess.showNetworkHelp(
                this);
    }

    private void launchWebShortcut(
            String target) {

        Uri uri =
                Uri.parse(target);

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        uri);

        intent.addCategory(
                Intent.CATEGORY_BROWSABLE);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent browserSelector =
                Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_BROWSER);

        ResolveInfo defaultBrowser =
                getPackageManager()
                        .resolveActivity(
                                browserSelector,
                                PackageManager.MATCH_DEFAULT_ONLY);

        if (defaultBrowser != null
                && defaultBrowser.activityInfo != null) {

            String browserPackage =
                    defaultBrowser
                            .activityInfo
                            .packageName;

            if (browserPackage != null
                    && !browserPackage.isEmpty()
                    && !"android".equals(
                            browserPackage)) {

                intent.setPackage(
                        browserPackage);
            }
        }

        try {
            startActivity(intent);

        } catch (RuntimeException exception) {
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    private void launchAppSettings(
            String target) {

        String packageTarget =
                target.startsWith(
                        "package:")
                        ? target
                        : "package:" + target;

        Intent intent =
                new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse(packageTarget));

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void launchApp(
            OverviewSection section,
            AppEntry app) {

        Intent launchIntent =
                new Intent(Intent.ACTION_MAIN);

        launchIntent.addCategory(
                Intent.CATEGORY_LAUNCHER);

        launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        launchIntent.setClassName(
                app.packageName,
                app.resolveInfo.activityInfo.name);

        try {
            startActivity(launchIntent);

            recordAppLaunch(
                    section,
                    app);

            return;

        } catch (RuntimeException ignored) {
        }

        Intent fallbackIntent;

        try {
            fallbackIntent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    app.packageName);

        } catch (RuntimeException exception) {
            fallbackIntent = null;
        }

        if (fallbackIntent != null) {
            fallbackIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            try {
                startActivity(
                        fallbackIntent);

                recordAppLaunch(
                        section,
                        app);

                return;

            } catch (RuntimeException ignored) {
            }
        }

        Toast.makeText(
                this,
                R.string.app_launch_failed,
                Toast.LENGTH_SHORT).show();
    }

    private void updatePageBackCallback() {

        OnBackInvokedDispatcher dispatcher =
                getOnBackInvokedDispatcher();

        if (!showingOverview
                && !pageBackCallbackRegistered) {

            dispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    pageBackCallback);

            pageBackCallbackRegistered = true;
            return;
        }

        if (showingOverview
                && pageBackCallbackRegistered) {

            dispatcher.unregisterOnBackInvokedCallback(
                    pageBackCallback);

            pageBackCallbackRegistered = false;
        }
    }

    private void setTopNavigation(
            boolean overview) {

        mainHeader.setVisibility(View.VISIBLE);

        statisticsManagement.setVisibility(
                View.GONE);

        if (sortingManagement != null) {
            sortingManagement.setVisibility(
                    View.GONE);
        }

        if (!PAGE_STATISTICS.equals(
                currentPage)) {

            setStatisticsSortMode(false);

            statisticsSortButton.setVisibility(
                    View.GONE);

            statisticsPeriodButton.setVisibility(
                    View.GONE);

            statisticsLimitButton.setVisibility(
                    View.GONE);

            statisticsResetButton.setVisibility(
                    View.GONE);
        }

        ViewGroup.MarginLayoutParams contentParams =
                (ViewGroup.MarginLayoutParams)
                        mainContentArea.getLayoutParams();

        contentParams.topMargin =
                getResources().getDimensionPixelSize(
                        R.dimen.spacing_lg);

        mainContentArea.setLayoutParams(contentParams);

        showingOverview = overview;
        updatePageBackCallback();

        if (overview) {
            drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    GravityCompat.END);

            topNavigationButton.setImageResource(
                    R.drawable.ic_menu);

            topNavigationButton.setContentDescription(
                    getString(
                            R.string.action_open_menu));

            topNavigationButton.setOnClickListener(v ->
                    drawerLayout.openDrawer(
                            GravityCompat.END));

            return;
        }

        drawerLayout.closeDrawer(
                GravityCompat.END);

        drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END);

        topNavigationButton.setImageResource(
                R.drawable.ic_arrow_back);

        topNavigationButton.setContentDescription(
                getString(
                        R.string.action_back_to_overview));

        topNavigationButton.setOnClickListener(v ->
                showOverview());
    }

    private String getAppVersionName() {
        try {
            android.content.pm.PackageInfo packageInfo;

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                packageInfo =
                        getPackageManager().getPackageInfo(
                                getPackageName(),
                                PackageManager.PackageInfoFlags.of(0));
            } else {
                packageInfo =
                        getPackageManager().getPackageInfo(
                                getPackageName(),
                                0);
            }

            return packageInfo.versionName != null
                    ? packageInfo.versionName
                    : "–";

        } catch (PackageManager.NameNotFoundException exception) {
            return "–";
        }
    }


    private void ensureSortingInflated() {

        if (sortingManagement != null) {
            return;
        }

        sortingManagement =
                (ScrollView) sortingStub.inflate();

        sortingModeGroup =
                sortingManagement.findViewById(
                        R.id.sortingModeGroup);

        sortingManualSettings =
                sortingManagement.findViewById(
                        R.id.sortingManualSettings);

        sortingSemiSettings =
                sortingManagement.findViewById(
                        R.id.sortingSemiSettings);

        sortingAutomaticSettings =
                sortingManagement.findViewById(
                        R.id.sortingAutomaticSettings);

        sortingTimeSettings =
                sortingManagement.findViewById(
                        R.id.sortingTimeSettings);

        sortingTimeDetails =
                sortingManagement.findViewById(
                        R.id.sortingTimeDetails);

        sortingSemiFavorites =
                sortingManagement.findViewById(
                        R.id.sortingSemiFavorites);

        sortingSemiCategories =
                sortingManagement.findViewById(
                        R.id.sortingSemiCategories);

        sortingSemiApps =
                sortingManagement.findViewById(
                        R.id.sortingSemiApps);

        sortingSemiShortcuts =
                sortingManagement.findViewById(
                        R.id.sortingSemiShortcuts);

        sortingTimeProfileEnabled =
                sortingManagement.findViewById(
                        R.id.sortingTimeProfileEnabled);

        sortingAlwaysStartFavorites =
                sortingManagement.findViewById(
                        R.id.sortingAlwaysStartFavorites);

        sortingSuggestionsEnabled =
                sortingManagement.findViewById(
                        R.id.sortingSuggestionsEnabled);

        sortingSuggestionInterval =
                sortingManagement.findViewById(
                        R.id.sortingSuggestionInterval);

        sortingSuggestionFavoriteCount =
                sortingManagement.findViewById(
                        R.id.sortingSuggestionFavoriteCount);

        sortingSuggestionCheckNow =
                sortingManagement.findViewById(
                        R.id.sortingSuggestionCheckNow);

        sortingDayStart =
                sortingManagement.findViewById(
                        R.id.sortingDayStart);

        sortingEveningStart =
                sortingManagement.findViewById(
                        R.id.sortingEveningStart);

        sortingFavoriteCount =
                sortingManagement.findViewById(
                        R.id.sortingFavoriteCount);

        sortingModeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingSemiFavorites.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingSemiCategories.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingSemiApps.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingSemiShortcuts.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingTimeProfileEnabled.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingAlwaysStartFavorites.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingSuggestionsEnabled.setOnCheckedChangeListener(
                (button, checked) -> {

                    if (!updatingSortingUi) {
                        saveSortingSettingsFromControls();
                    }
                });

        sortingDayStart.setOnClickListener(
                view ->
                        showSortingHourDialog(
                                true));

        sortingEveningStart.setOnClickListener(
                view ->
                        showSortingHourDialog(
                                false));

        sortingFavoriteCount.setOnClickListener(
                view ->
                        showAutomaticFavoriteCountDialog());

        sortingSuggestionFavoriteCount.setOnClickListener(
                view ->
                        showAutomaticFavoriteCountDialog());

        sortingSuggestionInterval.setOnClickListener(
                view ->
                        showSortingSuggestionIntervalDialog());

        sortingSuggestionCheckNow.setOnClickListener(
                view ->
                        requestSortingSuggestionCheckNow());

        sortingStub = null;
    }

    private void refreshSortingSettingsUi() {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        updatingSortingUi = true;

        if (settings.mode
                == SortingSettingsStore.Mode.SEMI_AUTOMATIC) {

            sortingModeGroup.check(
                    R.id.sortingModeSemi);

        } else if (settings.mode
                == SortingSettingsStore.Mode.AUTOMATIC) {

            sortingModeGroup.check(
                    R.id.sortingModeAutomatic);

        } else {
            sortingModeGroup.check(
                    R.id.sortingModeManual);
        }

        sortingSemiFavorites.setChecked(
                settings.semiFavorites);

        sortingSemiCategories.setChecked(
                settings.semiCategories);

        sortingSemiApps.setChecked(
                settings.semiApps);

        sortingSemiShortcuts.setChecked(
                settings.semiShortcuts);

        sortingTimeProfileEnabled.setChecked(
                settings.timeProfileEnabled);

        sortingAlwaysStartFavorites.setChecked(
                settings.alwaysStartFavorites);

        sortingSuggestionsEnabled.setChecked(
                settings.suggestionsEnabled);

        sortingSuggestionInterval.setText(
                getString(
                        R.string.sorting_suggestion_interval_value,
                        settings.suggestionIntervalDays));

        sortingSuggestionFavoriteCount.setText(
                getString(
                        R.string.sorting_suggestion_favorite_count_value,
                        settings.automaticFavoriteCount));

        sortingDayStart.setText(
                getString(
                        R.string.sorting_day_start_value,
                        formatSortingHour(
                                settings.dayStartHour)));

        sortingEveningStart.setText(
                getString(
                        R.string.sorting_evening_start_value,
                        formatSortingHour(
                                settings.eveningStartHour)));

        sortingFavoriteCount.setText(
                getString(
                        R.string.sorting_favorite_count_value,
                        settings.automaticFavoriteCount));

        updatingSortingUi = false;

        updateSortingModeVisibility(
                settings);
    }

    private void saveSortingSettingsFromControls() {

        if (sortingManagement == null
                || updatingSortingUi) {

            return;
        }

        SortingSettingsStore.Settings current =
                sortingSettingsStore.load();

        int selectedMode =
                sortingModeGroup
                        .getCheckedRadioButtonId();

        SortingSettingsStore.Mode mode;

        if (selectedMode
                == R.id.sortingModeSemi) {

            mode =
                    SortingSettingsStore.Mode
                            .SEMI_AUTOMATIC;

        } else if (selectedMode
                == R.id.sortingModeAutomatic) {

            mode =
                    SortingSettingsStore.Mode
                            .AUTOMATIC;

        } else {
            mode =
                    SortingSettingsStore.Mode.MANUAL;
        }

        SortingSettingsStore.Settings updated =
                new SortingSettingsStore.Settings(
                        mode,
                        sortingSemiFavorites.isChecked(),
                        sortingSemiCategories.isChecked(),
                        sortingSemiApps.isChecked(),
                        sortingSemiShortcuts.isChecked(),
                        sortingTimeProfileEnabled.isChecked(),
                        current.dayStartHour,
                        current.eveningStartHour,
                        current.automaticFavoriteCount,
                        sortingAlwaysStartFavorites.isChecked(),
                        sortingSuggestionsEnabled.isChecked(),
                        current.suggestionIntervalDays);

        boolean suggestionsJustEnabled =
                !current.suggestionsEnabled
                        && updated.suggestionsEnabled;

        if (current.mode
                == SortingSettingsStore.Mode.AUTOMATIC
                && updated.mode
                != SortingSettingsStore.Mode.AUTOMATIC) {

            materializeFullAutomaticState();
        }

        sortingSettingsStore.save(
                updated);

        if (suggestionsJustEnabled) {

            sortingSettingsStore
                    .markSuggestionHandledNow();
        }

        updateFavoriteEditingState();

        updateSortingModeVisibility(
                updated);

        refreshOverviewForSortingSettingsChange();
        scheduleNextSortingProfileBoundary();
    }

    private void updateSortingModeVisibility(
            SortingSettingsStore.Settings settings) {

        boolean manual =
                settings.mode
                        == SortingSettingsStore.Mode.MANUAL;

        boolean semiAutomatic =
                settings.mode
                        == SortingSettingsStore.Mode.SEMI_AUTOMATIC;

        boolean automatic =
                settings.mode
                        == SortingSettingsStore.Mode.AUTOMATIC;

        sortingManualSettings.setVisibility(
                manual
                        ? View.VISIBLE
                        : View.GONE);

        sortingSemiSettings.setVisibility(
                semiAutomatic
                        ? View.VISIBLE
                        : View.GONE);

        sortingAutomaticSettings.setVisibility(
                automatic
                        ? View.VISIBLE
                        : View.GONE);

        sortingTimeSettings.setVisibility(
                manual
                        ? View.GONE
                        : View.VISIBLE);

        sortingFavoriteCount.setVisibility(
                automatic
                        ? View.VISIBLE
                        : View.GONE);

        sortingTimeDetails.setVisibility(
                !manual
                        && settings.timeProfileEnabled
                        ? View.VISIBLE
                        : View.GONE);
    }

    private String formatSortingHour(
            int hour) {

        return String.format(
                Locale.ROOT,
                "%02d:00",
                hour);
    }

    private void showSortingHourDialog(
            boolean dayStart) {

        SortingSettingsStore.Settings current =
                sortingSettingsStore.load();

        CharSequence[] choices =
                new CharSequence[24];

        for (int hour = 0;
             hour < choices.length;
             hour++) {

            choices[hour] =
                    formatSortingHour(
                            hour);
        }

        int selected =
                dayStart
                        ? current.dayStartHour
                        : current.eveningStartHour;

        new AlertDialog.Builder(this)
                .setTitle(
                        dayStart
                                ? R.string.sorting_day_start_title
                                : R.string.sorting_evening_start_title)
                .setSingleChoiceItems(
                        choices,
                        selected,
                        (dialog, which) -> {

                            int other =
                                    dayStart
                                            ? current.eveningStartHour
                                            : current.dayStartHour;

                            if (which == other) {
                                Toast.makeText(
                                                this,
                                                R.string.sorting_time_same_error,
                                                Toast.LENGTH_SHORT)
                                        .show();

                                return;
                            }

                            SortingSettingsStore.Settings updated =
                                    new SortingSettingsStore.Settings(
                                            current.mode,
                                            current.semiFavorites,
                                            current.semiCategories,
                                            current.semiApps,
                                            current.semiShortcuts,
                                            current.timeProfileEnabled,
                                            dayStart
                                                    ? which
                                                    : current.dayStartHour,
                                            dayStart
                                                    ? current.eveningStartHour
                                                    : which,
                                            current.automaticFavoriteCount,
                                            current.alwaysStartFavorites,
                                            current.suggestionsEnabled,
                                            current.suggestionIntervalDays);

                            sortingSettingsStore.save(
                                    updated);

                            refreshSortingSettingsUi();

                            refreshOverviewForSortingSettingsChange();
                            scheduleNextSortingProfileBoundary();

                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private void showAutomaticFavoriteCountDialog() {

        SortingSettingsStore.Settings current =
                sortingSettingsStore.load();

        CharSequence[] choices =
                new CharSequence[28];

        for (int index = 0;
             index < choices.length;
             index++) {

            choices[index] =
                    Integer.toString(
                            index + 3);
        }

        int selected =
                current.automaticFavoriteCount >= 3
                        && current.automaticFavoriteCount <= 30
                        ? current.automaticFavoriteCount - 3
                        : -1;

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.sorting_favorite_count_title)
                .setSingleChoiceItems(
                        choices,
                        selected,
                        (dialog, which) -> {

                            int count =
                                    which + 3;

                            SortingSettingsStore.Settings updated =
                                    new SortingSettingsStore.Settings(
                                            current.mode,
                                            current.semiFavorites,
                                            current.semiCategories,
                                            current.semiApps,
                                            current.semiShortcuts,
                                            current.timeProfileEnabled,
                                            current.dayStartHour,
                                            current.eveningStartHour,
                                            count,
                                            current.alwaysStartFavorites,
                                            current.suggestionsEnabled,
                                            current.suggestionIntervalDays);

                            sortingSettingsStore.save(
                                    updated);

                            refreshSortingSettingsUi();

                            refreshOverviewForSortingSettingsChange();
                            scheduleNextSortingProfileBoundary();

                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private void showSortingSuggestionIntervalDialog() {

        SortingSettingsStore.Settings current =
                sortingSettingsStore.load();

        int[] values = {
                7,
                14,
                30,
                60,
                90
        };

        CharSequence[] choices = {
                getString(
                        R.string.sorting_suggestion_interval_7),
                getString(
                        R.string.sorting_suggestion_interval_14),
                getString(
                        R.string.sorting_suggestion_interval_30),
                getString(
                        R.string.sorting_suggestion_interval_60),
                getString(
                        R.string.sorting_suggestion_interval_90)
        };

        int selected =
                2;

        for (int i = 0;
             i < values.length;
             i++) {

            if (values[i]
                    == current.suggestionIntervalDays) {

                selected =
                        i;

                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.sorting_suggestion_interval_title)
                .setSingleChoiceItems(
                        choices,
                        selected,
                        (dialog, which) -> {

                            SortingSettingsStore.Settings updated =
                                    new SortingSettingsStore.Settings(
                                            current.mode,
                                            current.semiFavorites,
                                            current.semiCategories,
                                            current.semiApps,
                                            current.semiShortcuts,
                                            current.timeProfileEnabled,
                                            current.dayStartHour,
                                            current.eveningStartHour,
                                            current.automaticFavoriteCount,
                                            current.alwaysStartFavorites,
                                            current.suggestionsEnabled,
                                            values[which]);

                            sortingSettingsStore.save(
                                    updated);

                            sortingSettingsStore
                                    .markSuggestionHandledNow();

                            refreshSortingSettingsUi();

                            dialog.dismiss();
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private void requestSortingSuggestionCheckNow() {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (settings.mode
                != SortingSettingsStore.Mode.MANUAL) {

            return;
        }

        if (!appsLoaded) {

            sortingSuggestionManualCheckPending =
                    true;

            loadAppsAsync();

            Toast.makeText(
                            this,
                            R.string.sorting_suggestion_loading,
                            Toast.LENGTH_SHORT)
                    .show();

            return;
        }

        performSortingSuggestionCheck(
                true);
    }

    private void runPendingSortingSuggestionChecks() {

        if (!appsLoaded) {
            return;
        }

        if (sortingSuggestionManualCheckPending) {

            sortingSuggestionManualCheckPending =
                    false;

            sortingSuggestionAutomaticCheckPending =
                    false;

            performSortingSuggestionCheck(
                    true);

            return;
        }

        if (sortingSuggestionAutomaticCheckPending) {

            sortingSuggestionAutomaticCheckPending =
                    false;

            performSortingSuggestionCheck(
                    false);
        }
    }

    private void performSortingSuggestionCheck(
            boolean explicitCheck) {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        if (settings.mode
                != SortingSettingsStore.Mode.MANUAL) {

            return;
        }

        if (!explicitCheck
                && (!settings.suggestionsEnabled
                || !sortingSettingsStore
                        .isSuggestionDue(
                                System.currentTimeMillis(),
                                settings))) {

            return;
        }

        SortingSuggestionEngine.Plan plan =
                buildSortingSuggestionPlan();

        sortingSettingsStore
                .markSuggestionHandledNow();

        if (!plan.hasAnyChange()) {

            if (explicitCheck) {

                new AlertDialog.Builder(this)
                        .setMessage(
                                R.string.sorting_suggestion_up_to_date)
                        .setPositiveButton(
                                android.R.string.ok,
                                null)
                        .show();
            }

            return;
        }

        if (explicitCheck) {

            showSortingSuggestionPreview(
                    plan);

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.sorting_suggestion_available_title)
                .setMessage(
                        R.string.sorting_suggestion_available_message)
                .setPositiveButton(
                        R.string.sorting_suggestion_show,
                        (dialog, which) ->
                                showSortingSuggestionPreview(
                                        plan))
                .setNeutralButton(
                        R.string.sorting_suggestion_skip,
                        null)
                .setNegativeButton(
                        R.string.sorting_suggestion_disable,
                        (dialog, which) ->
                                disableSortingSuggestions())
                .show();
    }

    private void disableSortingSuggestions() {

        SortingSettingsStore.Settings current =
                sortingSettingsStore.load();

        SortingSettingsStore.Settings updated =
                new SortingSettingsStore.Settings(
                        current.mode,
                        current.semiFavorites,
                        current.semiCategories,
                        current.semiApps,
                        current.semiShortcuts,
                        current.timeProfileEnabled,
                        current.dayStartHour,
                        current.eveningStartHour,
                        current.automaticFavoriteCount,
                        current.alwaysStartFavorites,
                        false,
                        current.suggestionIntervalDays);

        sortingSettingsStore.save(
                updated);

        sortingSettingsStore
                .markSuggestionHandledNow();

        if (sortingManagement != null) {

            refreshSortingSettingsUi();
        }
    }

    private SortingSuggestionEngine.Plan
            buildSortingSuggestionPlan() {

        SortingSettingsStore.Settings settings =
                sortingSettingsStore.load();

        UsageStatisticsStore.SortingSnapshot sorting =
                usageStatisticsStore
                        .getSortingSnapshot(
                                UsageStatisticsStore.TimeProfile.DAY,
                                settings.dayStartHour,
                                settings.eveningStartHour);

        Map<String, Integer> itemScores =
                createItemScoreMap(
                        sorting.getOverallWeighted());

        List<String> sectionOrder =
                new ArrayList<>();

        for (OverviewSection section :
                overviewSections) {

            sectionOrder.add(
                    section.id);
        }

        Set<String> currentFavoriteIds =
                new HashSet<>();

        List<String> currentFavoriteItems =
                new ArrayList<>();

        for (AppEntry app :
                apps) {

            if (!favoritesStore.isFavorite(
                    app.packageName)) {

                continue;
            }

            String id =
                    SectionItemOrderStore
                            .appItemId(
                                    app.packageName);

            currentFavoriteIds.add(
                    id);

            currentFavoriteItems.add(
                    id);
        }

        List<ShortcutEntry> shortcuts =
                shortcutStore.getShortcuts();

        for (ShortcutEntry shortcut :
                shortcuts) {

            if (!shortcut.favorite) {
                continue;
            }

            String id =
                    SectionItemOrderStore
                            .shortcutItemId(
                                    shortcut.id);

            currentFavoriteIds.add(
                    id);

            currentFavoriteItems.add(
                    id);
        }

        Map<String, List<String>> sectionItems =
                new HashMap<>();

        sectionItems.put(
                "favorites",
                sectionItemOrderStore
                        .getOrderedIds(
                                "favorites",
                                currentFavoriteItems));

        for (CategoryEntry category :
                categoryStore.getCategories()) {

            String sectionId =
                    "category:"
                            + category.id;

            List<String> ids =
                    new ArrayList<>();

            for (AppEntry app :
                    apps) {

                if (categoryStore
                        .isAssignedToCategory(
                                app.packageName,
                                category.id)) {

                    ids.add(
                            SectionItemOrderStore
                                    .appItemId(
                                            app.packageName));
                }
            }

            for (ShortcutEntry shortcut :
                    shortcuts) {

                if (shortcut.categoryIds
                        .contains(
                                category.id)) {

                    ids.add(
                            SectionItemOrderStore
                                    .shortcutItemId(
                                            shortcut.id));
                }
            }

            sectionItems.put(
                    sectionId,
                    sectionItemOrderStore
                            .getOrderedIds(
                                    sectionId,
                                    ids));
        }

        List<String> shortcutSectionItems =
                new ArrayList<>();

        for (ShortcutEntry shortcut :
                shortcuts) {

            shortcutSectionItems.add(
                    SectionItemOrderStore
                            .shortcutItemId(
                                    shortcut.id));
        }

        sectionItems.put(
                "shortcuts",
                sectionItemOrderStore
                        .getOrderedIds(
                                "shortcuts",
                                shortcutSectionItems));

        List<String> allFavoriteCandidates =
                new ArrayList<>();

        for (AppEntry app :
                apps) {

            allFavoriteCandidates.add(
                    SectionItemOrderStore
                            .appItemId(
                                    app.packageName));
        }

        for (ShortcutEntry shortcut :
                shortcuts) {

            allFavoriteCandidates.add(
                    SectionItemOrderStore
                            .shortcutItemId(
                                    shortcut.id));
        }

        List<String> favoriteCandidateBaseline =
                sectionItemOrderStore
                        .getOrderedIds(
                                "favorites",
                                allFavoriteCandidates);

        return SortingSuggestionEngine.build(
                sectionOrder,
                sectionItems,
                currentFavoriteIds,
                favoriteCandidateBaseline,
                settings.automaticFavoriteCount,
                itemScores,
                sorting.getOverallSectionCounts());
    }

    private void showSortingSuggestionPreview(
            SortingSuggestionEngine.Plan plan) {

        View content =
                getLayoutInflater()
                        .inflate(
                                R.layout.dialog_sorting_suggestions,
                                null,
                                false);

        CheckBox favoriteOrder =
                content.findViewById(
                        R.id.suggestionApplyFavoriteOrder);

        View favoriteOrderComparison =
                content.findViewById(
                        R.id.suggestionFavoriteOrderComparison);

        TextView favoriteOrderCurrent =
                content.findViewById(
                        R.id.suggestionFavoriteOrderCurrent);

        TextView favoriteOrderPreview =
                content.findViewById(
                        R.id.suggestionFavoriteOrderPreview);

        CheckBox favoriteAssignment =
                content.findViewById(
                        R.id.suggestionApplyFavoriteAssignment);

        View favoriteAssignmentComparison =
                content.findViewById(
                        R.id.suggestionFavoriteAssignmentComparison);

        TextView favoriteAssignmentCurrent =
                content.findViewById(
                        R.id.suggestionFavoriteAssignmentCurrent);

        TextView favoriteAssignmentPreview =
                content.findViewById(
                        R.id.suggestionFavoriteAssignmentPreview);

        CheckBox categories =
                content.findViewById(
                        R.id.suggestionApplyCategories);

        View categoriesComparison =
                content.findViewById(
                        R.id.suggestionCategoriesComparison);

        TextView categoriesCurrent =
                content.findViewById(
                        R.id.suggestionCategoriesCurrent);

        TextView categoriesPreview =
                content.findViewById(
                        R.id.suggestionCategoriesPreview);

        CheckBox appsBox =
                content.findViewById(
                        R.id.suggestionApplyApps);

        View appsComparison =
                content.findViewById(
                        R.id.suggestionAppsComparison);

        TextView appsCurrent =
                content.findViewById(
                        R.id.suggestionAppsCurrent);

        TextView appsPreview =
                content.findViewById(
                        R.id.suggestionAppsPreview);

        CheckBox shortcutsBox =
                content.findViewById(
                        R.id.suggestionApplyShortcuts);

        View shortcutsComparison =
                content.findViewById(
                        R.id.suggestionShortcutsComparison);

        TextView shortcutsCurrent =
                content.findViewById(
                        R.id.suggestionShortcutsCurrent);

        TextView shortcutsPreview =
                content.findViewById(
                        R.id.suggestionShortcutsPreview);

        configureSortingSuggestionBlock(
                favoriteOrder,
                favoriteOrderComparison,
                favoriteOrderCurrent,
                favoriteOrderPreview,
                plan.favoriteOrderChanged(),
                formatSortingSuggestionItemOrder(
                        plan.currentFavoriteOrder),
                formatSortingSuggestionItemOrder(
                        plan.favoriteOrderPreview(
                                favoriteAssignment.isChecked())));

        favoriteAssignment.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        favoriteOrderPreview.setText(
                                formatSortingSuggestionItemOrder(
                                        plan.favoriteOrderPreview(
                                                isChecked))));

        configureSortingSuggestionBlock(
                favoriteAssignment,
                favoriteAssignmentComparison,
                favoriteAssignmentCurrent,
                favoriteAssignmentPreview,
                plan.favoriteAssignmentChanged(),
                formatSortingSuggestionFavoriteRemovals(
                        plan),
                formatSortingSuggestionFavoriteAdditions(
                        plan));

        configureSortingSuggestionBlock(
                categories,
                categoriesComparison,
                categoriesCurrent,
                categoriesPreview,
                plan.categoryOrderChanged(),
                formatSortingSuggestionSectionOrder(
                        plan.currentSectionOrder),
                formatSortingSuggestionSectionOrder(
                        plan.suggestedSectionOrder));

        configureSortingSuggestionBlock(
                appsBox,
                appsComparison,
                appsCurrent,
                appsPreview,
                plan.appOrderChanged(),
                formatSortingSuggestionChangedOrders(
                        plan,
                        plan.suggestedAppOrders,
                        true),
                formatSortingSuggestionChangedOrders(
                        plan,
                        plan.suggestedAppOrders,
                        false));

        configureSortingSuggestionBlock(
                shortcutsBox,
                shortcutsComparison,
                shortcutsCurrent,
                shortcutsPreview,
                plan.shortcutOrderChanged(),
                formatSortingSuggestionChangedOrders(
                        plan,
                        plan.suggestedShortcutOrders,
                        true),
                formatSortingSuggestionChangedOrders(
                        plan,
                        plan.suggestedShortcutOrders,
                        false));

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.sorting_suggestion_preview_title)
                        .setView(
                                content)
                        .setPositiveButton(
                                R.string.sorting_suggestion_apply,
                                null)
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        dialog.setOnShowListener(
                ignored -> {

                    /*
                     * Übernehmen verändert die bestehende
                     * manuelle Sortierung und verwendet daher
                     * dieselbe Danger-UI wie andere
                     * destruktive Bestätigungen.
                     */
                    styleCategoryDialog(
                            dialog,
                            true);

                    dialog.getButton(
                                        DialogInterface.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        view -> {

                                            boolean useFavoriteOrder =
                                                    plan.favoriteOrderChanged()
                                                            && favoriteOrder.isChecked();

                                            boolean useFavoriteAssignment =
                                                    plan.favoriteAssignmentChanged()
                                                            && favoriteAssignment.isChecked();

                                            boolean useCategories =
                                                    plan.categoryOrderChanged()
                                                            && categories.isChecked();

                                            boolean useApps =
                                                    plan.appOrderChanged()
                                                            && appsBox.isChecked();

                                            boolean useShortcuts =
                                                    plan.shortcutOrderChanged()
                                                            && shortcutsBox.isChecked();

                                            if (!useFavoriteOrder
                                                    && !useFavoriteAssignment
                                                    && !useCategories
                                                    && !useApps
                                                    && !useShortcuts) {

                                                Toast.makeText(
                                                                this,
                                                                R.string.sorting_suggestion_none_selected,
                                                                Toast.LENGTH_SHORT)
                                                        .show();

                                                return;
                                            }

                                            applySortingSuggestionPlan(
                                                    plan,
                                                    useFavoriteOrder,
                                                    useFavoriteAssignment,
                                                    useCategories,
                                                    useApps,
                                                    useShortcuts);

                                            dialog.dismiss();
                                        });
                });

        dialog.show();
    }

    private void configureSortingSuggestionBlock(
            CheckBox checkBox,
            View comparison,
            TextView currentPreview,
            TextView suggestedPreview,
            boolean visible,
            String currentText,
            String suggestedText) {

        checkBox.setVisibility(
                visible
                        ? View.VISIBLE
                        : View.GONE);

        comparison.setVisibility(
                visible
                        ? View.VISIBLE
                        : View.GONE);

        checkBox.setChecked(
                visible);

        currentPreview.setText(
                currentText);

        suggestedPreview.setText(
                suggestedText);
    }

    private String formatSortingSuggestionItemOrder(
            List<String> itemIds) {

        StringBuilder result =
                new StringBuilder();

        int position =
                1;

        for (String itemId :
                itemIds) {

            if (result.length() > 0) {
                result.append(
                        "\n");
            }

            result.append(
                    position)
                    .append(
                            ". ")
                    .append(
                            getSortingSuggestionItemLabel(
                                    itemId));

            position++;
        }

        return result.toString();
    }

    private String formatSortingSuggestionSectionOrder(
            List<String> sectionIds) {

        StringBuilder result =
                new StringBuilder();

        int position =
                1;

        for (String sectionId :
                sectionIds) {

            if (result.length() > 0) {
                result.append(
                        "\n");
            }

            result.append(
                    position)
                    .append(
                            ". ")
                    .append(
                            getSortingSuggestionSectionLabel(
                                    sectionId));

            position++;
        }

        return result.toString();
    }

    private String formatSortingSuggestionFavoriteRemovals(
            SortingSuggestionEngine.Plan plan) {

        StringBuilder result =
                new StringBuilder();

        for (String id :
                plan.currentFavoriteOrder) {

            if (plan.suggestedFavoriteIds
                    .contains(
                            id)) {

                continue;
            }

            if (result.length() > 0) {
                result.append(
                        "\n");
            }

            result.append(
                    "− ")
                    .append(
                            getSortingSuggestionItemLabel(
                                    id));
        }

        return result.length() == 0
                ? "\u2014"
                : result.toString();
    }

    private String formatSortingSuggestionFavoriteAdditions(
            SortingSuggestionEngine.Plan plan) {

        StringBuilder result =
                new StringBuilder();

        for (String id :
                plan.suggestedAssignedFavoriteOrder) {

            if (plan.currentFavoriteIds
                    .contains(
                            id)) {

                continue;
            }

            if (result.length() > 0) {
                result.append(
                        "\n");
            }

            result.append(
                    "+ ")
                    .append(
                            getSortingSuggestionItemLabel(
                                    id));
        }

        return result.length() == 0
                ? "\u2014"
                : result.toString();
    }

    private String formatSortingSuggestionChangedOrders(
            SortingSuggestionEngine.Plan plan,
            Map<String, List<String>> suggestedOrders,
            boolean currentState) {

        StringBuilder result =
                new StringBuilder();

        for (String sectionId :
                plan.currentSectionOrder) {

            if ("favorites".equals(
                    sectionId)) {

                continue;
            }

            List<String> current =
                    plan.currentSectionItemOrders
                            .get(
                                    sectionId);

            List<String> suggested =
                    suggestedOrders.get(
                            sectionId);

            if (current == null
                    || suggested == null
                    || current.equals(
                            suggested)) {

                continue;
            }

            if (result.length() > 0) {

                result.append(
                        "\n\n");
            }

            result.append(
                    getSortingSuggestionSectionLabel(
                            sectionId))
                    .append(
                            ":\n")
                    .append(
                            formatSortingSuggestionItemOrder(
                                    currentState
                                            ? current
                                            : suggested));
        }

        return result.toString();
    }

    private String getSortingSuggestionItemLabel(
            String itemId) {

        if (itemId.startsWith(
                "app:")) {

            String packageName =
                    itemId.substring(
                            "app:".length());

            for (AppEntry app :
                    apps) {

                if (app.packageName.equals(
                        packageName)) {

                    return app.label;
                }
            }

            return packageName;
        }

        if (itemId.startsWith(
                "shortcut:")) {

            String shortcutId =
                    itemId.substring(
                            "shortcut:".length());

            for (ShortcutEntry shortcut :
                    shortcutStore.getShortcuts()) {

                if (shortcut.id.equals(
                        shortcutId)) {

                    return shortcut.name;
                }
            }

            return shortcutId;
        }

        return itemId;
    }

    private String getSortingSuggestionSectionLabel(
            String sectionId) {

        for (OverviewSection section :
                overviewSections) {

            if (section.id.equals(
                    sectionId)) {

                return section.gridLabel;
            }
        }

        return sectionId;
    }

    private void applySortingSuggestionPlan(
            SortingSuggestionEngine.Plan plan,
            boolean applyFavoriteOrder,
            boolean applyFavoriteAssignment,
            boolean applyCategories,
            boolean applyApps,
            boolean applyShortcuts) {

        if (applyFavoriteAssignment) {

            Set<String> favoritePackages =
                    new HashSet<>();

            Set<String> favoriteShortcutIds =
                    new HashSet<>();

            for (String itemId :
                    plan.suggestedFavoriteIds) {

                if (itemId.startsWith(
                        "app:")) {

                    favoritePackages.add(
                            itemId.substring(
                                    "app:".length()));

                } else if (itemId.startsWith(
                        "shortcut:")) {

                    favoriteShortcutIds.add(
                            itemId.substring(
                                    "shortcut:".length()));
                }
            }

            favoritesStore.replaceAll(
                    favoritePackages);

            shortcutStore.replaceFavorites(
                    favoriteShortcutIds);

            List<String> favoriteOrder =
                    new ArrayList<>();

            if (applyFavoriteOrder) {

                favoriteOrder.addAll(
                        plan.suggestedAssignedFavoriteOrder);

            } else {

                Set<String> added =
                        new HashSet<>();

                for (String itemId :
                        plan.currentFavoriteOrder) {

                    if (plan.suggestedFavoriteIds
                            .contains(
                                    itemId)
                            && added.add(
                                    itemId)) {

                        favoriteOrder.add(
                                itemId);
                    }
                }

                for (String itemId :
                        plan.suggestedAssignedFavoriteOrder) {

                    if (added.add(
                            itemId)) {

                        favoriteOrder.add(
                                itemId);
                    }
                }
            }

            sectionItemOrderStore.saveOrder(
                    "favorites",
                    favoriteOrder);

        } else if (applyFavoriteOrder) {

            sectionItemOrderStore.saveOrder(
                    "favorites",
                    plan.suggestedFavoriteOrder);
        }

        if (applyCategories) {

            overviewOrderStore.saveOrderIds(
                    plan.suggestedSectionOrder);
        }

        if (applyApps
                || applyShortcuts) {

            for (String sectionId :
                    plan.currentSectionOrder) {

                if ("favorites".equals(
                        sectionId)) {

                    continue;
                }

                List<String> order;

                if (applyApps
                        && applyShortcuts) {

                    order =
                            plan.suggestedAllItemOrders
                                    .get(
                                            sectionId);

                } else if (applyApps) {

                    order =
                            plan.suggestedAppOrders
                                    .get(
                                            sectionId);

                } else {

                    order =
                            plan.suggestedShortcutOrders
                                    .get(
                                            sectionId);
                }

                if (order != null) {

                    sectionItemOrderStore.saveOrder(
                            sectionId,
                            order);
                }
            }
        }

        sortingSettingsStore
                .markSuggestionHandledNow();

        refreshOverviewForSortingSettingsChange();

        Toast.makeText(
                        this,
                        R.string.sorting_suggestion_applied,
                        Toast.LENGTH_SHORT)
                .show();
    }

    private void showSortingManagement() {

        currentPage = PAGE_SORTING;
        setTopNavigation(false);

        ensureSortingInflated();

        shortcutHelpButton.setVisibility(
                View.GONE);

        hideOverviewSortMode();

        categoryManagement.setVisibility(
                View.GONE);

        shortcutManagement.setVisibility(
                View.GONE);

        backupManagement.setVisibility(
                View.GONE);

        hideHelpPage();
        hideAboutPage();

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_sorting);

        refreshSortingSettingsUi();

        sortingManagement.setVisibility(
                View.VISIBLE);

        sortingManagement.scrollTo(
                0,
                0);

        drawerLayout.closeDrawer(
                GravityCompat.END);
    }

    private void ensureHelpInflated() {

        if (helpManagement != null) {
            return;
        }

        helpManagement =
                (ScrollView) helpStub.inflate();

        helpShortcutSection =
                helpManagement.findViewById(
                        R.id.helpShortcutSection);

        helpStub = null;
    }

    private void ensureAboutInflated() {

        if (aboutManagement != null) {
            return;
        }

        aboutManagement =
                (ScrollView) aboutStub.inflate();

        aboutVersion =
                aboutManagement.findViewById(
                        R.id.aboutVersion);

        aboutPackage =
                aboutManagement.findViewById(
                        R.id.aboutPackage);

        aboutManagement.findViewById(
                        R.id.aboutGithubButton)
                .setOnClickListener(v ->
                        openGithub());

        aboutManagement.findViewById(
                        R.id.aboutSupportButton)
                .setOnClickListener(v ->
                        openSupport());

        aboutStub = null;
    }

    private void hideHelpPage() {

        if (helpManagement != null) {
            helpManagement.setVisibility(
                    View.GONE);
        }
    }

    private void hideAboutPage() {

        if (aboutManagement != null) {
            aboutManagement.setVisibility(
                    View.GONE);
        }
    }

    private void showAbout() {
        currentPage = PAGE_ABOUT;
        setTopNavigation(false);

        ensureAboutInflated();

        shortcutHelpButton.setVisibility(View.GONE);
        hideOverviewSortMode();

        categoryManagement.setVisibility(View.GONE);
        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        hideHelpPage();

        overviewList.setVisibility(View.GONE);
        appList.setVisibility(View.GONE);
        pageMessage.setVisibility(View.GONE);

        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        pageTitle.setText(R.string.nav_about);

        aboutVersion.setText(
                getString(
                        R.string.about_version,
                        getAppVersionName()));

        aboutPackage.setText(
                getString(
                        R.string.about_package,
                        getPackageName()));

        aboutManagement.setVisibility(View.VISIBLE);
        aboutManagement.scrollTo(0, 0);

        drawerLayout.closeDrawer(GravityCompat.END);
    }

    private void openGithub() {
        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                getString(
                                        R.string.about_github_url)));

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (RuntimeException exception) {
            Toast.makeText(
                    this,
                    R.string.about_github_failed,
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void openSupport() {
        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                getString(
                                        R.string.about_support_url)));

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (RuntimeException exception) {
            Toast.makeText(
                    this,
                    R.string.about_support_failed,
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void showHelp(
            boolean jumpToShortcuts) {

        currentPage = PAGE_HELP;
        setTopNavigation(false);

        ensureHelpInflated();
        hideAboutPage();

        shortcutHelpButton.setVisibility(
                View.GONE);

        hideOverviewSortMode();

        categoryManagement.setVisibility(
                View.GONE);

        shortcutManagement.setVisibility(
                View.GONE);

        backupManagement.setVisibility(
                View.GONE);

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_help);

        helpManagement.setVisibility(
                View.VISIBLE);

        drawerLayout.closeDrawer(
                GravityCompat.END);

        helpManagement.post(() -> {
            if (jumpToShortcuts) {
                helpManagement.smoothScrollTo(
                        0,
                        helpShortcutSection.getTop());
            } else {
                helpManagement.scrollTo(0, 0);
            }
        });
    }

    private void showBackupManagement() {
        currentPage = PAGE_BACKUP;
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(
                View.GONE);

        hideOverviewSortMode();

        categoryManagement.setVisibility(
                View.GONE);

        shortcutManagement.setVisibility(
                View.GONE);

        hideHelpPage();

        hideAboutPage();

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_backup);

        backupManagement.setVisibility(
                View.VISIBLE);

        drawerLayout.closeDrawer(
                GravityCompat.END);
    }

    private void createBackup() {
        Intent intent =
                new Intent(
                        Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                "application/json");

        intent.putExtra(
                Intent.EXTRA_TITLE,
                "AppStow-backup.json");

        try {
            startActivityForResult(
                    intent,
                    REQUEST_CREATE_BACKUP);

        } catch (RuntimeException exception) {
            Toast.makeText(
                    this,
                    R.string.backup_failed,
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void selectBackupForRestore() {
        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                "application/json");

        try {
            startActivityForResult(
                    intent,
                    REQUEST_RESTORE_BACKUP);

        } catch (RuntimeException exception) {
            Toast.makeText(
                    this,
                    R.string.backup_restore_failed,
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private static void clearPendingRestoreResult(
            Context context) {

        context.getSharedPreferences(
                        BACKUP_RUNTIME_PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .remove(
                        KEY_RESTORE_RESULT_PENDING)
                .remove(
                        KEY_RESTORE_RESULT_SUCCESS)
                .remove(
                        KEY_RESTORE_RESULT_MESSAGE)
                .apply();
    }

    private static String getRestoreErrorMessage(
            Exception exception) {

        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return exception.getClass()
                    .getSimpleName();
        }

        return message;
    }

    private static void publishRestoreResult(
            Context context,
            boolean success,
            String errorMessage) {

        SharedPreferences.Editor editor =
                context.getSharedPreferences(
                                BACKUP_RUNTIME_PREFS,
                                Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(
                                KEY_RESTORE_RESULT_PENDING,
                                true)
                        .putBoolean(
                                KEY_RESTORE_RESULT_SUCCESS,
                                success);

        if (errorMessage == null
                || errorMessage.trim().isEmpty()) {

            editor.remove(
                    KEY_RESTORE_RESULT_MESSAGE);

        } else {
            editor.putString(
                    KEY_RESTORE_RESULT_MESSAGE,
                    errorMessage);
        }

        editor.commit();

        Intent intent =
                new Intent(
                        ACTION_RESTORE_FINISHED);

        intent.setPackage(
                context.getPackageName());

        context.sendBroadcast(
                intent);
    }

    private void handlePendingRestoreResult() {

        SharedPreferences preferences =
                getSharedPreferences(
                        BACKUP_RUNTIME_PREFS,
                        MODE_PRIVATE);

        if (!preferences.getBoolean(
                KEY_RESTORE_RESULT_PENDING,
                false)) {

            return;
        }

        boolean success =
                preferences.getBoolean(
                        KEY_RESTORE_RESULT_SUCCESS,
                        false);

        String errorMessage =
                preferences.getString(
                        KEY_RESTORE_RESULT_MESSAGE,
                        null);

        preferences.edit()
                .remove(
                        KEY_RESTORE_RESULT_PENDING)
                .remove(
                        KEY_RESTORE_RESULT_SUCCESS)
                .remove(
                        KEY_RESTORE_RESULT_MESSAGE)
                .apply();

        CharSequence resultMessage;

        if (success) {
            resultMessage =
                    getString(
                            R.string.backup_restored);

        } else if (errorMessage == null
                || errorMessage.trim().isEmpty()) {

            resultMessage =
                    getString(
                            R.string.backup_restore_failed);

        } else {
            resultMessage =
                    getString(
                            R.string.backup_restore_failed)
                            + "\n"
                            + errorMessage;
        }

        Toast.makeText(
                this,
                resultMessage,
                Toast.LENGTH_LONG)
                .show();

        if (success
                && !isFinishing()
                && !isDestroyed()) {

            recreate();
        }
    }

    private void confirmBackupRestore(
            JSONObject backup) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.backup_restore_title)
                        .setMessage(
                                R.string.backup_restore_message)
                        .setPositiveButton(
                                R.string.backup_restore_confirm,
                                (currentDialog, which) -> {
                                    Context appContext =
                                            getApplicationContext();

                                    clearPendingRestoreResult(
                                            appContext);

                                    backupExecutor.execute(() -> {
                                        boolean success;
                                        String errorMessage =
                                                null;

                                        try {
                                            BackupManager.restoreBackup(
                                                    appContext,
                                                    backup);

                                            success = true;

                                        } catch (Exception exception) {
                                            success = false;
                                            errorMessage =
                                                    getRestoreErrorMessage(
                                                            exception);
                                        }

                                        publishRestoreResult(
                                                appContext,
                                                success,
                                                errorMessage);
                                    });
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(
                dialog,
                true);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data);

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri =
                data.getData();

        if (requestCode
                == REQUEST_CREATE_BACKUP) {

            backupExecutor.execute(() -> {
                boolean success;

                try {
                    BackupManager.writeBackup(
                            this,
                            uri);

                    success = true;

                } catch (Exception exception) {
                    success = false;
                }

                boolean backupSucceeded =
                        success;

                runOnUiThread(() -> {
                    if (isFinishing()
                            || isDestroyed()) {

                        return;
                    }

                    Toast.makeText(
                            this,
                            backupSucceeded
                                    ? R.string.backup_created
                                    : R.string.backup_failed,
                            Toast.LENGTH_LONG)
                            .show();
                });
            });

            return;
        }

        if (requestCode
                == REQUEST_RESTORE_BACKUP) {

            backupExecutor.execute(() -> {
                JSONObject backup;

                try {
                    backup =
                            BackupManager.readBackup(
                                    this,
                                    uri);

                } catch (Exception exception) {
                    String errorMessage =
                            getRestoreErrorMessage(
                                    exception);

                    runOnUiThread(() -> {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        Toast.makeText(
                                this,
                                getString(
                                        R.string.backup_restore_failed)
                                        + "\n"
                                        + errorMessage,
                                Toast.LENGTH_LONG)
                                .show();
                    });

                    return;
                }

                JSONObject validatedBackup =
                        backup;

                runOnUiThread(() -> {
                    if (isFinishing()
                            || isDestroyed()) {

                        return;
                    }

                    confirmBackupRestore(
                            validatedBackup);
                });
            });
        }
    }

}
