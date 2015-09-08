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

import com.example.guestbook.model.subOrder.OrderType;

import java.lang.String;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An order object representing an AppDirect subscription order. Its boiled down
 * into editionCode, pricingDuration, and a map of units to quantities (NOTE: if
 * this changes from AppDirect will have to redo). TODO: remove public on fields
 * BE YE WARNED: Due to no definitive schema definitions, and the use of generated JAXB
 * classes from example XML which is very similar but different enough to
 * warrant different types, we have similar constructors dealing with similar
 * types.
 **/
@Entity
@Data
@NoArgsConstructor
public class Order {

	@Id
	public Long id;
	public String editionCode;
	public String pricingDuration;
	public Map<String, String> orderItems = new HashMap<>();

	/**
	 * see class description warning
	 * @param ot
	 */
	public Order(OrderType ot) {
		id = null;
		editionCode = ot.getEditionCode();
		pricingDuration = ot.getPricingDuration();
		orderItems = createItemMapFromListForSubOrder(ot.getItem());
	}

	private Map createItemMapFromListForSubOrder(List<com.example.guestbook.model.subOrder.ItemType> itemTypeList) {
		HashMap<String, String> theMap = new HashMap<String, String>();
		for (com.example.guestbook.model.subOrder.ItemType it : itemTypeList) {
			String theValue = new Integer(it.getQuantity()).toString();
			theMap.put(it.getUnit(), theValue);
		}
		return theMap;
	}

	/**
	 * see class description warning
	 * @param ot
	 */
	public Order(com.example.guestbook.model.subChange.OrderType ot) {
		id = null;
		editionCode = ot.getEditionCode();
		pricingDuration = ot.getPricingDuration();
		orderItems = createItemMapFromListForSubChange(ot.getItem());
	}

	private Map createItemMapFromListForSubChange(List<com.example.guestbook.model.subChange.ItemType> itemTypeList) {
		HashMap<String, String> theMap = new HashMap<String, String>();
		for (com.example.guestbook.model.subChange.ItemType it : itemTypeList) {
			String theValue = new Integer(it.getQuantity()).toString();
			theMap.put(it.getUnit(), theValue);
		}
		return theMap;
	}
}
