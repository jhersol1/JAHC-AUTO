package com.jahc.auto.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ScheduleDao_Impl implements ScheduleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Schedule> __insertionAdapterOfSchedule;

  private final EntityDeletionOrUpdateAdapter<Schedule> __deletionAdapterOfSchedule;

  private final SharedSQLiteStatement __preparedStmtOfMarkSent;

  public ScheduleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSchedule = new EntityInsertionAdapter<Schedule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `schedules` (`id`,`title`,`target`,`contactName`,`phone`,`message`,`hour`,`minute`,`repeatDaily`,`daysOfWeek`,`enabled`,`createdAt`,`lastSentAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Schedule entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getTarget() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTarget());
        }
        if (entity.getContactName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getContactName());
        }
        if (entity.getPhone() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPhone());
        }
        if (entity.getMessage() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getMessage());
        }
        statement.bindLong(7, entity.getHour());
        statement.bindLong(8, entity.getMinute());
        final int _tmp = entity.getRepeatDaily() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getDaysOfWeek() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getDaysOfWeek());
        }
        final int _tmp_1 = entity.getEnabled() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        statement.bindLong(12, entity.getCreatedAt());
        if (entity.getLastSentAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getLastSentAt());
        }
      }
    };
    this.__deletionAdapterOfSchedule = new EntityDeletionOrUpdateAdapter<Schedule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `schedules` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Schedule entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfMarkSent = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE schedules SET lastSentAt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final Schedule schedule, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSchedule.insertAndReturnId(schedule);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Schedule schedule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSchedule.handle(schedule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSent(final long id, final long ts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSent.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, ts);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSent.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Schedule>> observeAll() {
    final String _sql = "SELECT * FROM schedules ORDER BY hour, minute";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"schedules"}, new Callable<List<Schedule>>() {
      @Override
      @NonNull
      public List<Schedule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfRepeatDaily = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDaily");
          final int _cursorIndexOfDaysOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "daysOfWeek");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSentAt");
          final List<Schedule> _result = new ArrayList<Schedule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Schedule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpTarget;
            if (_cursor.isNull(_cursorIndexOfTarget)) {
              _tmpTarget = null;
            } else {
              _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            }
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpPhone;
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null;
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final boolean _tmpRepeatDaily;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRepeatDaily);
            _tmpRepeatDaily = _tmp != 0;
            final String _tmpDaysOfWeek;
            if (_cursor.isNull(_cursorIndexOfDaysOfWeek)) {
              _tmpDaysOfWeek = null;
            } else {
              _tmpDaysOfWeek = _cursor.getString(_cursorIndexOfDaysOfWeek);
            }
            final boolean _tmpEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpLastSentAt;
            if (_cursor.isNull(_cursorIndexOfLastSentAt)) {
              _tmpLastSentAt = null;
            } else {
              _tmpLastSentAt = _cursor.getLong(_cursorIndexOfLastSentAt);
            }
            _item = new Schedule(_tmpId,_tmpTitle,_tmpTarget,_tmpContactName,_tmpPhone,_tmpMessage,_tmpHour,_tmpMinute,_tmpRepeatDaily,_tmpDaysOfWeek,_tmpEnabled,_tmpCreatedAt,_tmpLastSentAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllOnce(final Continuation<? super List<Schedule>> $completion) {
    final String _sql = "SELECT * FROM schedules";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Schedule>>() {
      @Override
      @NonNull
      public List<Schedule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfRepeatDaily = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDaily");
          final int _cursorIndexOfDaysOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "daysOfWeek");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSentAt");
          final List<Schedule> _result = new ArrayList<Schedule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Schedule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpTarget;
            if (_cursor.isNull(_cursorIndexOfTarget)) {
              _tmpTarget = null;
            } else {
              _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            }
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpPhone;
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null;
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final boolean _tmpRepeatDaily;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRepeatDaily);
            _tmpRepeatDaily = _tmp != 0;
            final String _tmpDaysOfWeek;
            if (_cursor.isNull(_cursorIndexOfDaysOfWeek)) {
              _tmpDaysOfWeek = null;
            } else {
              _tmpDaysOfWeek = _cursor.getString(_cursorIndexOfDaysOfWeek);
            }
            final boolean _tmpEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpLastSentAt;
            if (_cursor.isNull(_cursorIndexOfLastSentAt)) {
              _tmpLastSentAt = null;
            } else {
              _tmpLastSentAt = _cursor.getLong(_cursorIndexOfLastSentAt);
            }
            _item = new Schedule(_tmpId,_tmpTitle,_tmpTarget,_tmpContactName,_tmpPhone,_tmpMessage,_tmpHour,_tmpMinute,_tmpRepeatDaily,_tmpDaysOfWeek,_tmpEnabled,_tmpCreatedAt,_tmpLastSentAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getEnabled(final Continuation<? super List<Schedule>> $completion) {
    final String _sql = "SELECT * FROM schedules WHERE enabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Schedule>>() {
      @Override
      @NonNull
      public List<Schedule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfRepeatDaily = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDaily");
          final int _cursorIndexOfDaysOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "daysOfWeek");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSentAt");
          final List<Schedule> _result = new ArrayList<Schedule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Schedule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpTarget;
            if (_cursor.isNull(_cursorIndexOfTarget)) {
              _tmpTarget = null;
            } else {
              _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            }
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpPhone;
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null;
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final boolean _tmpRepeatDaily;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRepeatDaily);
            _tmpRepeatDaily = _tmp != 0;
            final String _tmpDaysOfWeek;
            if (_cursor.isNull(_cursorIndexOfDaysOfWeek)) {
              _tmpDaysOfWeek = null;
            } else {
              _tmpDaysOfWeek = _cursor.getString(_cursorIndexOfDaysOfWeek);
            }
            final boolean _tmpEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpLastSentAt;
            if (_cursor.isNull(_cursorIndexOfLastSentAt)) {
              _tmpLastSentAt = null;
            } else {
              _tmpLastSentAt = _cursor.getLong(_cursorIndexOfLastSentAt);
            }
            _item = new Schedule(_tmpId,_tmpTitle,_tmpTarget,_tmpContactName,_tmpPhone,_tmpMessage,_tmpHour,_tmpMinute,_tmpRepeatDaily,_tmpDaysOfWeek,_tmpEnabled,_tmpCreatedAt,_tmpLastSentAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final long id, final Continuation<? super Schedule> $completion) {
    final String _sql = "SELECT * FROM schedules WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Schedule>() {
      @Override
      @Nullable
      public Schedule call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfRepeatDaily = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDaily");
          final int _cursorIndexOfDaysOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "daysOfWeek");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSentAt");
          final Schedule _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpTarget;
            if (_cursor.isNull(_cursorIndexOfTarget)) {
              _tmpTarget = null;
            } else {
              _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            }
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpPhone;
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null;
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final boolean _tmpRepeatDaily;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRepeatDaily);
            _tmpRepeatDaily = _tmp != 0;
            final String _tmpDaysOfWeek;
            if (_cursor.isNull(_cursorIndexOfDaysOfWeek)) {
              _tmpDaysOfWeek = null;
            } else {
              _tmpDaysOfWeek = _cursor.getString(_cursorIndexOfDaysOfWeek);
            }
            final boolean _tmpEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpLastSentAt;
            if (_cursor.isNull(_cursorIndexOfLastSentAt)) {
              _tmpLastSentAt = null;
            } else {
              _tmpLastSentAt = _cursor.getLong(_cursorIndexOfLastSentAt);
            }
            _result = new Schedule(_tmpId,_tmpTitle,_tmpTarget,_tmpContactName,_tmpPhone,_tmpMessage,_tmpHour,_tmpMinute,_tmpRepeatDaily,_tmpDaysOfWeek,_tmpEnabled,_tmpCreatedAt,_tmpLastSentAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
