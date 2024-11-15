/*
 * The XML:DB Initiative Software License, Version 1.0
 *
 * Copyright (c) 2000-2025 The XML:DB Initiative. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must include the
 * following acknowledgment: "This product includes software developed by the XML:DB Initiative
 * (http://www.xmldb.org/)." Alternately, this acknowledgment may appear in the software itself, if
 * and wherever such third-party acknowledgments normally appear.
 *
 * 4. The name "XML:DB Initiative" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please contact info@xmldb.org.
 *
 * 5. Products derived from this software may not be called "XML:DB", nor may "XML:DB" appear in
 * their name, without prior written permission of the XML:DB Initiative.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * =================================================================================================
 * This software consists of voluntary contributions made by many individuals on behalf of the
 * XML:DB Initiative. For more information on the XML:DB Initiative, please see
 * <https://github.com/xmldb-org/>
 */
package org.xmldb.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;

public class TestDatabase extends ConfigurableImpl implements Database {
  private static final String DEFAULT_NAME = "testdatabase";
  private static Database registeredDatabase;

  static {
    try {
      register();
    } catch (XMLDBException e) {
      throw new IllegalStateException(e);
    }
  }

  private final String name;
  private final Map<String, TestCollection> collections;

  public TestDatabase() {
    this(null);
  }

  public TestDatabase(String name) {
    if (name == null || name.isEmpty()) {
      this.name = DEFAULT_NAME;
    } else {
      this.name = name;
    }
    collections = new HashMap<>();
  }

  @Override
  public final String getName() throws XMLDBException {
    return name;
  }

  public TestCollection addCollection(String collectionName) {
    return collections.computeIfAbsent(collectionName, TestCollection::create);
  }

  @Override
  public Collection getCollection(String uri, Properties info) throws XMLDBException {
    return collections.get(uri);
  }

  @Override
  public boolean acceptsURI(String uri) {
    return uri.startsWith(DatabaseManager.URI_PREFIX + "test");
  }

  @Override
  public String getConformanceLevel() throws XMLDBException {
    return "0";
  }

  /**
   * Returns if this database is registered against the {@link DatabaseManager}.
   * 
   * @return {@code true} if the driver is registered against {@link DatabaseManager}
   */
  public static boolean isRegistered() {
    return registeredDatabase != null;
  }

  /**
   * Register the driver against {@link DatabaseManager}. This is done automatically when the class
   * is loaded. Dropping the driver from DatabaseManager's list is possible using
   * {@link #deregister()} method.
   *
   * @throws IllegalStateException if the database is already registered
   * @throws XMLDBException if registering the database fails
   */
  public static void register() throws XMLDBException {
    if (isRegistered()) {
      throw new IllegalStateException(
          "Database is already registered. It can only be registered once.");
    }
    registeredDatabase = new TestDatabase(DEFAULT_NAME);
    DatabaseManager.registerDatabase(registeredDatabase, null);
  }

  /**
   * According to XML:DB specification, this driver is registered against {@link DatabaseManager}
   * when the class is loaded. To avoid leaks, this method allows unregistering the database so that
   * the class can be gc'ed if necessary.
   *
   * @throws IllegalStateException if the database is not registered
   */
  public static void deregister() {
    if (registeredDatabase != null) {
      DatabaseManager.deregisterDatabase(registeredDatabase);
      registeredDatabase = null;
    }
  }
}
