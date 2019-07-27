/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.serialization;

import java.time.*;
import java.util.Vector;

import org.slf4j.*;

/**
 * A specfic serialization handler for {@link Vector}.
 * @author Laurent Cohen
 */
public class JavaTimeSerializationHandler extends AbstractSerializationHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(Serializer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();

  @Override
  public void writeObject(final Object obj, final Serializer serializer, final ClassDescriptor cd) throws Exception {
    if (traceEnabled) log.trace("writing declared fields for cd={}", cd);
    if (obj instanceof LocalDateTime) writeLocalDateTime(serializer, (LocalDateTime) obj);
    else if (obj instanceof LocalDate) writeLocalDate(serializer, (LocalDate) obj);
    else if (obj instanceof LocalTime) writeLocalTime(serializer, (LocalTime) obj);
    else if (obj instanceof ZonedDateTime) writeZonedDateTime(serializer, (ZonedDateTime) obj);
    else if (obj instanceof ZoneId) writeZoneId(serializer, (ZoneId) obj);
    else serializer.writeDeclaredFields(obj, cd);
  }

  @Override
  public Object readDObject(final Deserializer deserializer, final ClassDescriptor cd) throws Exception {
    if (traceEnabled) log.trace("reading declared fields for cd={}", cd);
    if (cd.clazz == LocalDateTime.class) return readLocalDateTime(deserializer);
    else if (cd.clazz == LocalDate.class) return readLocalDate(deserializer);
    else if (cd.clazz == LocalTime.class) return readLocalTime(deserializer);
    else if (cd.clazz == ZonedDateTime.class) return readZonedDateTime(deserializer);
    else if (ZoneId.class.isAssignableFrom(cd.clazz)) return readZoneId(deserializer);
    final Object obj = newInstance(cd);
    deserializer.readDeclaredFields(cd, obj);
    return obj;
  }

  /**
   * 
   * @param serializer the serializer to use.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  private static void writeLocalDateTime(final Serializer serializer, final LocalDateTime obj) throws Exception {
    writeLocalTime(serializer, obj.toLocalTime());
    writeLocalDate(serializer, obj.toLocalDate());
  }

  /**
   * 
   * @param deserializer the deserializer to use.
   * @return a deserialized object.
   * @throws Exception if any error occurs.
   */
  private static LocalDateTime readLocalDateTime(final Deserializer deserializer) throws Exception {
    final LocalTime time = readLocalTime(deserializer);
    final LocalDate date = readLocalDate(deserializer);
    return LocalDateTime.of(date, time);
  }

  /**
   * 
   * @param serializer the serializer to use.
   * @param obj the object to write.
   * @throws Exception if any error occurs.
   */
  private static void writeZonedDateTime(final Serializer serializer, final ZonedDateTime obj) throws Exception {
    writeLocalTime(serializer, obj.toLocalTime());
    writeLocalDate(serializer, obj.toLocalDate());
    writeZoneId(serializer, obj.getZone());
  }

  /**
   * 
   * @param deserializer the deserializer to use.
   * @return a deserialized object.
   * @throws Exception if any error occurs.
   */
  private static ZonedDateTime readZonedDateTime(final Deserializer deserializer) throws Exception {
    final LocalTime time = readLocalTime(deserializer);
    final LocalDate date = readLocalDate(deserializer);
    final ZoneId zoneId = readZoneId(deserializer);
    return ZonedDateTime.of(date, time, zoneId);
  }

  /**
   * 
   * @param serializer the serializer to use.
   * @param obj the object for which to write the fields.
   * @throws Exception if any error occurs.
   */
  private static void writeZoneId(final Serializer serializer, final ZoneId obj) throws Exception {
    serializer.writeString(obj.getId());
  }

  /**
   * 
   * @param deserializer the deserializer to use.
   * @return a deserialized object.
   * @throws Exception if any error occurs.
   */
  private static ZoneId readZoneId(final Deserializer deserializer) throws Exception {
    final String id = deserializer.readString();
    return ZoneId.of(id);
  }

  /**
   * 
   * @param serializer the serializer to use.
   * @param obj the object for which to write the fields.
   * @throws Exception if any error occurs.
   */
  private static void writeLocalDate(final Serializer serializer, final LocalDate obj) throws Exception {
    serializer.writeInt(obj.getYear());
    serializer.out.writeByte((byte) obj.getMonthValue());
    serializer.out.writeByte((byte) obj.getDayOfMonth());
  }

  /**
   * 
   * @param deserializer the deserializer to use.
   * @return a LocalDate
   * @throws Exception if any error occurs.
   */
  private static LocalDate readLocalDate(final Deserializer deserializer) throws Exception {
    final int year = deserializer.readInt();
    final int month = deserializer.in.readByte();
    final int day = deserializer.in.readByte();
    return LocalDate.of(year, month, day);
  }

  /**
   * 
   * @param serializer the serializer to use.
   * @param time the object for which to write the fields.
   * @throws Exception if any error occurs.
   */
  private static void writeLocalTime(final Serializer serializer, final LocalTime time) throws Exception {
    serializer.out.writeByte(time.getHour());
    serializer.out.writeByte(time.getMinute());
    serializer.out.writeByte(time.getSecond());
    serializer.writeInt(time.getNano());
  }

  /**
   * 
   * @param deserializer the deserializer to use.
   * @throws Exception if any error occurs.
   * @return a LocalTime.
   */
  private static LocalTime readLocalTime(final Deserializer deserializer) throws Exception {
    final int hour = deserializer.in.readByte();
    final int minute = deserializer.in.readByte();
    final int second = deserializer.in.readByte();
    final int nano = deserializer.readInt();
    return LocalTime.of(hour, minute, second, nano);
  }
}
