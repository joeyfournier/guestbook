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
import com.googlecode.objectify.annotation.Parent;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.guestbook.model.subOrder.CompanyType;
import com.googlecode.objectify.Key;

import java.lang.String;
import java.util.Date;
import java.util.List;

/**
 * A company who orders a subscription 
 * TODO: remove public on fields
 **/
@Entity
@Data
@NoArgsConstructor
public class Company {
  @Id 
  public Long id;
  @Index
  public String email;
  public String name;
  public String country;
  public String phoneNumber;
  public String website;
  @Index
  public String uuid;


  /** */
	public static Key<Company> key(long id) {
		return Key.create(Company.class, id);
	}


public Company(CompanyType ct) {
	id=null;
	email = ct.getEmail();
	name = ct.getName();
	country = ct.getCountry();
	phoneNumber = ct.getPhoneNumber();
	website = ct.getWebsite();
	uuid = ct.getWebsite();
}

}
