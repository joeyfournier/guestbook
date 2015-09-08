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

import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.guestbook.model.subOrder.MarketplaceType;

import java.lang.String;


/**
 * The @Entity tells Objectify about our entity.  We also register it in OfyHelper.java -- very
 * important. Our primary key @Id is set automatically by the Google Datastore for us.
 *
 *
 * respresents a AppDirect subscription marketplace
 **/
@Data
@NoArgsConstructor
@Entity
public class MarketPlace {
  @Id public Long id;
  public String baseUrl;
  public String partner;

  public MarketPlace(MarketplaceType mpt){
	  id = null;
	  baseUrl = mpt.getBaseUrl();
	  partner = mpt.getPartner();
  }
}
