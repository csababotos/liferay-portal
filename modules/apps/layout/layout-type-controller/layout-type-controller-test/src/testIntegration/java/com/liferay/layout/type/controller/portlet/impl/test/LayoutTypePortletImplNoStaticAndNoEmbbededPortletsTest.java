/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.layout.type.controller.portlet.impl.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;
import com.liferay.portal.kernel.service.PortletLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.util.test.LayoutTestUtil;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuel de la Peña
 */
@RunWith(Arquillian.class)
public class LayoutTypePortletImplNoStaticAndNoEmbbededPortletsTest
	extends LayoutTypePortletImplBaseTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Test
	public void testShouldReturnFalseIfANonlayoutCacheableRootPortletIsInstalled()
		throws Exception {

		Portlet noncacheablePortlet = PortletLocalServiceUtil.getPortletById(
			PortletKeys.LOGIN);

		LayoutTestUtil.addPortletToLayout(
			TestPropsValues.getUserId(), layout,
			noncacheablePortlet.getPortletId(), "column-1",
			new HashMap<String, String[]>());

		String cacheablePortletId = PortletProviderUtil.getPortletId(
			"com.liferay.journal.model.JournalArticle",
			PortletProvider.Action.ADD);

		LayoutTestUtil.addPortletToLayout(
			TestPropsValues.getUserId(), layout, cacheablePortletId, "column-1",
			new HashMap<String, String[]>());

		Assert.assertFalse(layoutTypePortlet.isCacheable());
	}

	@Test
	public void testShouldReturnTrueIfInstalledRootPortletsAreLayoutCacheable()
		throws Exception {

		String[] layoutStaticPortletsAll =
			PropsValues.LAYOUT_STATIC_PORTLETS_ALL;

		PropsUtil.set(PropsKeys.LAYOUT_STATIC_PORTLETS_ALL, "");

		try {
			String cacheablePortletId = PortletProviderUtil.getPortletId(
				"com.liferay.journal.model.JournalArticle",
				PortletProvider.Action.ADD);

			LayoutTestUtil.addPortletToLayout(
				TestPropsValues.getUserId(), layout, cacheablePortletId,
				"column-1", new HashMap<String, String[]>());

			Assert.assertTrue(layoutTypePortlet.isCacheable());
		}
		finally {
			PropsUtil.set(
				PropsKeys.LAYOUT_STATIC_PORTLETS_ALL,
				StringUtil.merge(layoutStaticPortletsAll, StringPool.COMMA));
		}
	}

}