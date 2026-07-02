/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package net.runelite.client.externalplugins;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;

@Singleton
@SuppressWarnings({"unused", "SameReturnValue"})
public class ExternalPluginManager
{
	private static Class<? extends Plugin>[] builtinExternals;

	private final PluginManager pluginManager;

	@Inject
	private ExternalPluginManager(PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}

	public static PluginHubManifest.JarData getJarData(Class<? extends Plugin> plugin)
	{
		return null;
	}

	public static PluginHubManifest.DisplayData getDisplayData(Class<? extends Plugin> plugin)
	{
		return null;
	}

	public static String getInternalName(Class<? extends Plugin> plugin)
	{
		return plugin.getName();
	}

	@SafeVarargs
	public static void loadBuiltin(Class<? extends Plugin>... plugins)
	{
		builtinExternals = plugins;
	}

	public void loadExternalPlugins() throws PluginInstantiationException
	{
		if (builtinExternals != null)
		{
			pluginManager.loadPlugins(Lists.newArrayList(builtinExternals), null);
		}
	}

	public List<String> getInstalledExternalPlugins()
	{
		return Collections.emptyList();
	}

	public void install(String key)
	{
	}

	public void remove(String key)
	{
	}

	public void update()
	{
	}
}
