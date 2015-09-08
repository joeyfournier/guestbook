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

package com.example.guestbook;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import lombok.Data;


/**
 * This is a simplified holder of AppDirect subscription and access data.
 * 
 * The @Entity tells Objectify about our entity.  We also register it in
 * OfyHelper.java -- very important.
 *
 */
@Data
@Entity
public class Subscription {
	
  @Id public Long id;
  @Index public String accountId;
  
  /*
  @Load
  Ref<Person> creator;
  */
  
  public Person creator;
  
  public Company company;
  public Order order;
  public String status;
  //public List<String> theUsers = new ArrayList<String>();
  public Map <String,User> theUsers = new HashMap<>();
  @Index public Date date;
  
  /**
   * Simple constructor just sets the date
   **/
  public Subscription() {
    date = new Date();
  }

  
  /**
   * Takes all important fields
   **/
  public Subscription(String aid, Person person, Company comp, Order ord , String stat, Map<String,User> us) {
    this();
    accountId = aid;
    creator = person;
    company = comp;
    status = stat;
    order = ord;
    theUsers = us;
  }

  
}
