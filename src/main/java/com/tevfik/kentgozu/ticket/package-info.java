@ApplicationModule(
		displayName = "Ticket",
		allowedDependencies = {
				"user::domain",
				"user::persistence",
				"geospatial",
				"ai_analysis::api",
				"ai_analysis::dto"
		})
package com.tevfik.kentgozu.ticket;

import org.springframework.modulith.ApplicationModule;
