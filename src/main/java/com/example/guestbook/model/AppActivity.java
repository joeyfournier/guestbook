/**
 * Copyright 2014-2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.guestbook.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
//import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.Key;

import java.lang.String;
import java.util.Date;
import java.util.List;

/**
 * The @Entity tells Objectify about our entity.  We also register it in OfyHelper.java -- very
 * important. Our primary key @Id is set automatically by the Google Datastore for us.
 * 
 * AppActivity is a simple holder of activity that is happening for an application. We us it 
 * to record any AppDirect events (i.e. activity) just to keep a simple record, for debug and
 * info purposes only.
 **/

@Entity
public class AppActivity {
  @Id public Long id; // a unique id for this entry

  public String action; // what action took place i.e. SUBSCRIPTION, etc.
  public String customer; // what is the customer the action took place on
  public String version; // what version of the s/w did the action take place on
  public String details; // general details, use to store entire xml?
  @Index public Date date; // date the action took place on

  /**
   * Simple constructor just sets the date
   **/
  public AppActivity (){
    date = new Date();
  }

  

  /**
   * Takes all important fields
   **/
  public AppActivity(String act, String cust, String ver, String det) {
    this();
    action = act;
    customer =cust;
    version = ver;
    details = det;
  }

}
