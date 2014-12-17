/*-
 * #%L
 * Slice - Core API
 * %%
 * Copyright (C) 2012 Cognifide Limited
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.cognifide.slice.api.context;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface RequestContextProvider {
	/**
	 * Return context provider that stores data in the request attributes. Bindings are stored separately for
	 * each injector.
	 * 
	 * @param injectorName Injector name
	 * @return Context provider that stores data in the request attribute.
	 */
	ContextProvider getContextProvider(String injectorName);
}
