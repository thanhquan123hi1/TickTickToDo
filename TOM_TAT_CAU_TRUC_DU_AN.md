# Tom tat cau truc du an TickTickToDo

Tai lieu nay tong hop chi tiet cac thu muc/file hien co trong workspace `D:\TickTickToDo`, mo ta chuc nang cua tung thanh phan va moi lien he giua chung.

## 1) Tong quan kien truc

Ung dung la Android app (Java + XML) theo kieu tach lop don gian:

- `UI/Presentation`: `Activity`, `BottomSheet`, `Adapter`, layout XML.
- `Navigation`: cac class dieu khien nav rail + side panel.
- `Domain/controller`: `TaskListController` dieu phoi viec load task theo tung che do xem.
- `Data`: Room (`Entity`, `Dao`, `AppDatabase`).
- `State`: `TaskViewModel` noi UI voi data bang `LiveData`.

### Luong du lieu chinh

`MainActivity/CalendarActivity` -> `TaskListController` (hoac goi truc tiep) -> `TaskViewModel` -> `TaskDao/CategoryDao` -> `AppDatabase` -> Room SQLite.

Khi nguoi dung sua/tao task:

`TaskDetailBottomSheet` -> `AppDatabase.databaseWriteExecutor` -> `TaskDao.insert/update/delete` -> LiveData cap nhat nguoc lai UI.

### Luong dieu huong chinh

- `MainActivity` nhan su kien rail button.
- `PanelContentFactory` tao danh sach muc trong panel mo rong.
- `NavRailController` doi state selected button.
- `NavPanel` mo/dong panel + overlay.
- Callback `PanelContentFactory.NavPanelCallback` goi `MainActivity.navigateTo(...)` de doi bo loc/task view.

---

## 2) Cay thu muc tong quan

```text
TickTickToDo/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  local.properties
  gradle/
    libs.versions.toml
    wrapper/
      gradle-wrapper.jar
      gradle-wrapper.properties
  app/
    build.gradle.kts
    proguard-rules.pro
    src/
      main/
        AndroidManifest.xml
        java/hcmute/edu/vn/ticktick/
          MainActivity.java
          CalendarActivity.java
          CountdownActivity.java
          adapter/TaskAdapter.java
          countdown/AlarmPlayer.java
          countdown/CountdownTimer.java
          database/AppDatabase.java
          database/Category.java
          database/CategoryDao.java
          database/Task.java
          database/TaskDao.java
          main/TaskListController.java
          navigation/NavPanel.java
          navigation/NavRailController.java
          navigation/PanelContentFactory.java
          navigation/PanelItemBuilder.java
          ui/DateUtils.java
          ui/TaskDetailBottomSheet.java
          ui/TaskViewModel.java
        res/
          layout/*.xml
          menu/drawer_menu.xml
          values/*.xml
          values-night/themes.xml
          color/*.xml
          drawable/*
          mipmap-*/ic_launcher*
          xml/*.xml
      test/java/.../ExampleUnitTest.java
      androidTest/java/.../ExampleInstrumentedTest.java
    build/ (file sinh tu Gradle, khong sua tay)
  build/ (bao cao build toan project)
```

Luu y:
- Thu muc `.git/`, `.gradle/`, `.idea/` la thu muc he thong/cong cu.
- Thu muc `app/build/` va `build/` la file phat sinh trong qua trinh build.

---

## 3) Giai thich tung file theo nhom

## 3.1 File cau hinh root project

### `settings.gradle.kts`
- Khai bao repository plugin/dependency (`google`, `mavenCentral`, `gradlePluginPortal`).
- Dat ten project goc: `TickTick`.
- Include module: `:app`.

### `build.gradle.kts`
- File build cap project.
- Khai bao Android Application plugin thong qua version catalog alias.

### `gradle.properties`
- Tham so chung cho Gradle daemon (`org.gradle.jvmargs`).
- Bat AndroidX (`android.useAndroidX=true`).
- Bat non-transitive R class.

### `local.properties`
- Duong dan SDK local tren may dev (`sdk.dir`).
- Khong nen commit VCS.

### `gradle/libs.versions.toml`
- Version catalog: quan ly version va toa do dependency.
- Chua cac lib chinh: Material, Room, RecyclerView, Lifecycle, DrawerLayout, SplashScreen, JUnit, Espresso.

### `gradle/wrapper/gradle-wrapper.properties`
- Cau hinh Gradle wrapper distribution (`gradle-9.1.0-bin.zip`) + checksum.

### `gradle/wrapper/gradle-wrapper.jar`
- Nhi phan wrapper de chay Gradle thong qua script wrapper.

### `gradlew` / `gradlew.bat`
- Script wrapper cho macOS/Linux (`gradlew`) va Windows (`gradlew.bat`).
- Muc tieu: chay build/clean/test ma khong can cai Gradle global.

### `build/reports/problems/problems-report.html`
- Bao cao van de build/cau hinh do Gradle sinh ra.

---

## 3.2 Module `app` (cau hinh + manifest)

### `app/build.gradle.kts`
- Cau hinh Android module:
  - `namespace`, `applicationId`: `hcmute.edu.vn.ticktick`
  - `compileSdk/minSdk/targetSdk = 36`
  - Java 11.
- Dang ky dependency runtime + test.
- Bat Room annotation processor.

### `app/proguard-rules.pro`
- Rule ProGuard/R8 custom (hien dang mostly mac dinh/chua them rule dac thu).

### `app/src/main/AndroidManifest.xml`
- Khai bao app-level:
  - Icon (`@mipmap/ic_plashscreen`), theme (`@style/Theme.TickTick`).
  - Backup rules (`@xml/data_extraction_rules`, `@xml/backup_rules`).
- Khai bao 3 Activity:
  - `MainActivity` (launcher, splash theme).
  - `CalendarActivity` (parent = MainActivity).
  - `CountdownActivity` (parent = MainActivity).

---

## 3.3 Java source code (chi tiet tung file)

### `app/src/main/java/hcmute/edu/vn/ticktick/MainActivity.java`
Vai tro:
- Activity trung tam man hinh task + navigation rail + expanded side panel.

Chuc nang chinh:
- Bind toan bo view tu `activity_main.xml`.
- Khoi tao collaborators:
  - `TaskViewModel`
  - `TaskAdapter`
  - `TaskListController`
  - `NavPanel`, `NavRailController`, `PanelContentFactory`
- Xu ly click rail button de mo panel hoac chuyen nhanh qua view `COMPLETED`.
- Implement callback `navigateTo(ViewDestination, categoryId)` de doi bo loc va title toolbar.
- Mo `TaskDetailBottomSheet` de tao/sua task.
- Cap nhat empty-state (`tvEmpty`) khi danh sach rong.
- Handle back press: neu panel dang mo thi dong panel, neu khong thi back mac dinh.

Moi lien he:
- Trung tam ket noi `navigation/*`, `main/TaskListController`, `ui/TaskDetailBottomSheet`, `adapter/TaskAdapter`.

### `app/src/main/java/hcmute/edu/vn/ticktick/CalendarActivity.java`
Vai tro:
- Man hinh lich, hien task theo ngay duoc chon.

Chuc nang chinh:
- Su dung `CalendarView` de chon ngay.
- Truy van `TaskViewModel.getTasksForDate(start,end)`.
- Day du lieu vao `TaskAdapter.setFlatData(dateLabel, tasks)`.
- Cho phep them task moi voi `dueDate` mac dinh = ngay dang chon.
- Mo `TaskDetailBottomSheet` de sua task.

Moi lien he:
- UI layout: `activity_calendar.xml`.
- Data: `TaskViewModel` -> `TaskDao.getTasksForDate`.

### `app/src/main/java/hcmute/edu/vn/ticktick/CountdownActivity.java`
Vai tro:
- Man hinh dong ho dem nguoc.

Chuc nang chinh:
- Quan ly state UI (input mode/timer mode).
- Nhan callback `CountdownTimer.Listener` (`onTick`, `onFinish`).
- Dieu khien cac nut Start/Pause/Resume/Reset.
- Dung `AlarmPlayer` de phat chuong khi het gio.
- Ho tro preset chip: 1/5/10/25 phut.

Moi lien he:
- UI layout: `activity_countdown.xml`.
- Logic timer tach rieng trong `countdown/CountdownTimer.java`.

### `app/src/main/java/hcmute/edu/vn/ticktick/adapter/TaskAdapter.java`
Vai tro:
- Adapter cho RecyclerView task, ho tro 2 loai row: section header + task item.

Chuc nang chinh:
- `setGroupedData(today, tomorrow, upcoming)` cho view nhom.
- `setFlatData(sectionTitle, tasks)` cho view loc phang.
- `isEmpty()` kiem tra danh sach co task that su hay chi header.
- Trong `TaskViewHolder.bind(...)`:
  - Strike-through task hoan thanh.
  - Hien/An mo ta.
  - Hien due time/date qua `DateUtils.getDisplayDateOrTime`.
  - To mau overdue.
  - Forward su kien check/click qua callback interface.

Moi lien he:
- Layout dung: `item_section_header.xml`, `item_task.xml`.
- Duoc su dung boi `MainActivity`, `CalendarActivity`.

### `app/src/main/java/hcmute/edu/vn/ticktick/countdown/CountdownTimer.java`
Vai tro:
- Dong goi logic dem nguoc (start/pause/resume/reset/destroy).

Chuc nang chinh:
- Dung Android `CountDownTimer` ben trong.
- Luu `totalTimeMillis`, `timeRemainingMillis`, `isRunning`, `isPaused`.
- Phat callback qua interface `Listener`.

Moi lien he:
- Duoc `CountdownActivity` su dung, khong phu thuoc UI truc tiep.

### `app/src/main/java/hcmute/edu/vn/ticktick/countdown/AlarmPlayer.java`
Vai tro:
- Play/stop am thanh bao thuc.

Chuc nang chinh:
- Lay default alarm URI, fallback notification URI.
- Tao `MediaPlayer`, play va release an toan.

Moi lien he:
- Duoc `CountdownActivity` goi khi `onFinish()` timer.

### `app/src/main/java/hcmute/edu/vn/ticktick/database/Task.java`
Vai tro:
- Room Entity `tasks`.

Truong du lieu:
- `id`, `title`, `description`, `dueDate`, `dueTime`, `priority`, `categoryId`, `completed`, `createdAt`.
- Co foreign key toi `Category` qua `categoryId` (`onDelete = SET_NULL`).

### `app/src/main/java/hcmute/edu/vn/ticktick/database/Category.java`
Vai tro:
- Room Entity `categories`.

Truong du lieu:
- `id`, `name`, `iconName`.
- `toString()` tra ve `name` (ho tro hien thi spinner/list).

### `app/src/main/java/hcmute/edu/vn/ticktick/database/TaskDao.java`
Vai tro:
- Room DAO cho task.

Chuc nang query noi bat:
- CRUD: `insert/update/delete`.
- Smart views: today, tomorrow, upcoming, next 7 days.
- Filters: inbox, unscheduled, completed, this week.
- Theo category: `getTasksByCategory`, `getTaskCountByCategory`.
- Calendar: `getTasksForDate(start,end)`.

### `app/src/main/java/hcmute/edu/vn/ticktick/database/CategoryDao.java`
Vai tro:
- Room DAO cho category.

Chuc nang:
- CRUD category.
- `getAllCategories()` (LiveData) va `getAllCategoriesSync()` (dong bo, dung trong background thread).

### `app/src/main/java/hcmute/edu/vn/ticktick/database/AppDatabase.java`
Vai tro:
- Singleton Room database cua app.

Chuc nang chinh:
- Khai bao entity: `Task`, `Category`.
- Cung cap `taskDao()` va `categoryDao()`.
- Cung cap thread pool static `databaseWriteExecutor` de ghi DB background.
- Callback `onCreate` pre-populate category mac dinh tieng Viet.

### `app/src/main/java/hcmute/edu/vn/ticktick/main/TaskListController.java`
Vai tro:
- Controller trung gian giua `MainActivity` va `TaskViewModel/TaskAdapter`.

Chuc nang chinh:
- Moi che do xem co 1 ham `load...` rieng: `loadNext7Days`, `loadToday`, `loadInbox`, `loadCategory`, `loadThisWeek`, `loadUnscheduled`, `loadCompleted`.
- Quan ly observer: remove observer cu truoc khi gan observer moi.
- Gui callback empty-state ve Activity.

Moi lien he:
- Duoc tao va su dung boi `MainActivity`.
- Goi query qua `TaskViewModel` va day adapter.

### `app/src/main/java/hcmute/edu/vn/ticktick/navigation/NavPanel.java`
Vai tro:
- Quan ly animation/open-close expanded panel + overlay + rail background.

### `app/src/main/java/hcmute/edu/vn/ticktick/navigation/NavRailController.java`
Vai tro:
- Quan ly visual selected state cua rail buttons.

### `app/src/main/java/hcmute/edu/vn/ticktick/navigation/PanelItemBuilder.java`
Vai tro:
- Factory tao view item trong panel (icon + label), divider, section title.

### `app/src/main/java/hcmute/edu/vn/ticktick/navigation/PanelContentFactory.java`
Vai tro:
- Build noi dung panel theo tung section: Tasks, Calendar, Filter, Tools, Settings, Profile.

Chuc nang chinh:
- Enum `ViewDestination` la tap dich den logic cho main list.
- Goi callback `navigateTo(...)` cho destination data-view.
- Mo Activity rieng cho Tools (`CalendarActivity`, `CountdownActivity`).
- Co mot so muc hien tai chi `Toast` (settings/profile/priority high).

### `app/src/main/java/hcmute/edu/vn/ticktick/ui/TaskViewModel.java`
Vai tro:
- AndroidViewModel cap API du lieu cho UI.

Chuc nang:
- Wrap query `TaskDao`/`CategoryDao` thanh cac ham de UI goi.
- Ham ghi DB (`insertTask/updateTask/deleteTask`) chay background qua executor.

### `app/src/main/java/hcmute/edu/vn/ticktick/ui/TaskDetailBottomSheet.java`
Vai tro:
- Form tao/sua/xoa task dang bottom sheet.

Chuc nang chinh:
- Input tieu de/mo ta/date/time/category/priority.
- Tai danh muc tu DB (`getAllCategoriesSync`) tren background thread.
- `saveTask()` validate tieu de, map spinner -> `categoryId`, insert/update DB.
- `deleteTask()` xoa task dang edit.
- Callback `OnTaskSavedListener` de Activity reload danh sach.

Moi lien he:
- Layout dung: `bottom_sheet_task_detail.xml`.
- Duoc goi tu `MainActivity` va `CalendarActivity`.

### `app/src/main/java/hcmute/edu/vn/ticktick/ui/DateUtils.java`
Vai tro:
- Tien ich ngay gio: format + tinh moc thoi gian.

Chuc nang chinh:
- `formatDate`, `formatFullDate`, `getDisplayDateOrTime`.
- Moc truy van: start/end today, tomorrow, week, 7 days.
- `isSameDay`, `getVietnameseDayOfWeek`.

---

## 3.4 Resource XML (layout/menu/theme/string/color)

### Layout files

#### `app/src/main/res/layout/activity_main.xml`
- Man hinh chinh:
  - phan content (toolbar + add bar + recycler + fab),
  - nav rail ben trai,
  - overlay dim,
  - expanded panel truot.
- La layout trung tam cho `MainActivity`.

#### `app/src/main/res/layout/activity_calendar.xml`
- Man hinh lich: toolbar, `CalendarView`, label ngay duoc chon, recycler task, empty state, FAB add.
- Duoc dung boi `CalendarActivity`.

#### `app/src/main/res/layout/activity_countdown.xml`
- Man hinh dem nguoc: input number pickers, hien thi timer, progress bar, button control, chip preset.
- Duoc dung boi `CountdownActivity`.

#### `app/src/main/res/layout/bottom_sheet_task_detail.xml`
- Form task detail trong bottom sheet (title/description/date/time/category/priority + action buttons).
- Duoc dung boi `TaskDetailBottomSheet`.

#### `app/src/main/res/layout/item_task.xml`
- Item row cua task list: checkbox + title/description + due time/date.
- Duoc dung boi `TaskAdapter.TaskViewHolder`.

#### `app/src/main/res/layout/item_section_header.xml`
- Item row cho section header (indicator + title + count + divider).
- Duoc dung boi `TaskAdapter.HeaderViewHolder`.

#### `app/src/main/res/layout/nav_header.xml`
- Header kieu NavigationView truyen thong (icon + ten app + slogan).
- Hien tai khong thay duoc gan truc tiep vao `MainActivity` rail tu code.

### Menu

#### `app/src/main/res/menu/drawer_menu.xml`
- Dinh nghia menu drawer co group: smart views, lists, filters, tools.
- Cau truc menu nay giong mo hinh nav truyen thong (NavigationView).
- Trong implementation hien tai, app dang dung rail + panel custom; file nay co the la tai nguyen du phong/tu phien ban cu.

### Values

#### `app/src/main/res/values/strings.xml`
- Toan bo text UI tieng Viet: nav, category, filter, button, message, countdown.

#### `app/src/main/res/values/colors.xml`
- Bang mau chu dao (vang/nau), mau trang thai (priority/completed/overdue), mau nav rail/panel.

#### `app/src/main/res/values/themes.xml`
- Theme app (`Theme.TickTick`) + style nav item shape selector + splash theme.
- Co comment tieng Viet chen trong XML (khong anh huong runtime neu hop le parser, nhung nen dung comment XML chuan).

#### `app/src/main/res/values-night/themes.xml`
- Placeholder cho dark theme (chua customize sau).

### Color selectors

#### `app/src/main/res/color/nav_item_selector.xml`
- Selector mau nen item theo trang thai checked.

#### `app/src/main/res/color/nav_item_icon_selector.xml`
- Selector mau icon theo checked/unchecked.

#### `app/src/main/res/color/nav_item_text_selector.xml`
- Selector mau text theo checked/unchecked.

### XML backup rules

#### `app/src/main/res/xml/backup_rules.xml`
- Mẫu full-backup-content (chua custom include/exclude).

#### `app/src/main/res/xml/data_extraction_rules.xml`
- Mẫu rule cloud backup/device transfer cho Android 12+ (chua custom).

---

## 3.5 Drawable va launcher assets

### Background/shape drawable
- `app/src/main/res/drawable/bg_avatar_circle.xml`: nen tron cho avatar.
- `app/src/main/res/drawable/bg_panel_item_ripple.xml`: ripple item trong expanded panel.
- `app/src/main/res/drawable/bg_panel_rounded.xml`: nen panel bo goc ben phai.
- `app/src/main/res/drawable/bg_rail_btn_ripple.xml`: background ripple mac dinh rail button.
- `app/src/main/res/drawable/bg_rail_btn_selected.xml`: background khi rail button duoc chon.

### Icon drawable (vector/png)
Cac file icon chuc nang giao dien:
- `ic_add.xml`, `ic_delete.xml`, `ic_check.xml`, `ic_clock.xml`.
- `ic_today.xml`, `ic_week.xml`, `ic_calendar.xml`, `ic_timer.xml`.
- `ic_inbox.xml`, `ic_filter.xml`, `ic_completed.xml`.
- `ic_work.xml`, `ic_study.xml`, `ic_travel.xml`, `ic_list.xml`, `ic_star.xml`.
- `ic_settings.xml`, `ic_menu.xml`, `ic_avatar_placeholder.xml`.
- `ic_launcher_background.xml`, `ic_launcher_foreground.xml`.
- `ic_plashscreen.png` (drawable) va `ic_plashscreen.png` (mipmap-anydpi) dung cho splash/icon.

### Mipmap launcher
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
- `app/src/main/res/mipmap-hdpi|mdpi|xhdpi|xxhdpi|xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi|mdpi|xhdpi|xxhdpi|xxxhdpi/ic_launcher_round.webp`

Vai tro:
- Cung cap launcher icon theo nhieu mat do man hinh.

---

## 3.6 Test source

### `app/src/test/java/hcmute/edu/vn/ticktick/ExampleUnitTest.java`
- Unit test mau mac dinh (`2 + 2 = 4`).

### `app/src/androidTest/java/hcmute/edu/vn/ticktick/ExampleInstrumentedTest.java`
- Instrumented test mau kiem tra package name app.

---

## 3.7 Build output folders (tu dong sinh)

### `app/build/`
- Chua output build cua module app: generated sources, intermediates, dex, merged resources, manifest merge, apk debug, ...
- Khong sua tay; duoc tao lai sau moi lan build.

### `build/`
- Build output cap project (bao gom reports).

### `.gradle/`
- Cache/trang thai Gradle local.

### `.idea/`
- Cau hinh IDE.

### `.git/`
- Du lieu version control.

---

## 4) Moi lien he giua cac file (ban do phu thuoc)

## 4.1 UI task list chinh

1. `activity_main.xml` dinh nghia layout.
2. `MainActivity` bind view + khoi tao collaborator.
3. `PanelContentFactory` phat sinh su kien dieu huong -> `MainActivity.navigateTo(...)`.
4. `TaskListController` dang ky observer phu hop voi che do xem.
5. `TaskViewModel` goi query tu `TaskDao`.
6. Ket qua `LiveData<List<Task>>` day vao `TaskAdapter`.
7. `TaskAdapter` render `item_section_header.xml` + `item_task.xml`.

## 4.2 Them/sua/xoa task

1. Tu `MainActivity`/`CalendarActivity`, mo `TaskDetailBottomSheet`.
2. `TaskDetailBottomSheet` hien thi form `bottom_sheet_task_detail.xml`.
3. Nguoi dung chon date/time/category/priority.
4. Bottom sheet ghi DB qua `TaskDao` trong executor.
5. Observer o Activity tu dong nhan du lieu moi va render lai.

## 4.3 Calendar

1. `CalendarActivity` dung `activity_calendar.xml`.
2. Khi doi ngay trong `CalendarView`, goi `getTasksForDate(start,end)`.
3. `TaskAdapter.setFlatData` hien task cua ngay do.

## 4.4 Countdown

1. `CountdownActivity` dung `activity_countdown.xml`.
2. Start/Pause/Resume/Reset thong qua `CountdownTimer`.
3. Het gio -> `AlarmPlayer.play()` + dialog thong bao.

## 4.5 Navigation rail + panel

1. Rail button click tai `MainActivity`.
2. `NavRailController` doi background selected.
3. `PanelContentFactory` build item view bang `PanelItemBuilder`.
4. `NavPanel` open/close + animation + overlay.

---

## 5) Nhan xet nhanh ve trang thai hien tai

- Cau truc da tach class kha ro theo trach nhiem (UI, navigation, data, utility).
- Co 2 mo hinh menu song song:
  - Rail/panel custom dang duoc su dung.
  - `drawer_menu.xml` + `nav_header.xml` co ve la tai nguyen kiem du phong/cu.
- Test hien tai moi la mau mac dinh, chua co test nghiep vu cho `TaskListController`, `DateUtils`, DAO query.

---

## 6) Danh sach file duoc thong ke trong tai lieu nay

Tai lieu da mo ta toan bo file nguon/chinh duoi day:
- Root config: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`, `gradlew`, `gradlew.bat`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`.
- App config: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`.
- Java source: 19 file trong package `hcmute.edu.vn.ticktick`.
- Resource source: 57 file trong `res` (layout/menu/values/color/drawable/mipmap/xml).
- Test source: 2 file mau (`test` va `androidTest`).
- Build/report folder: `app/build/`, `build/reports/problems/problems-report.html`.

Neu ban muon, toi co the tao them **phien ban 2** cua tai lieu nay theo dinh dang bang (1 dong/1 file) de tra cuu nhanh hon.
