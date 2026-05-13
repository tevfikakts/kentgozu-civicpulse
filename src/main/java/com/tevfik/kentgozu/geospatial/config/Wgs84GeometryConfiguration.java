package com.tevfik.kentgozu.geospatial.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WGS 84 (GPS) dünya referansı; EPSG {@value #WGS_84_SRID}. Bu fabrika ile üretilen tüm geometriler
 * aynı SRID ile işaretlenir — konum üretiminde çift {@code double} enlem/boylam alanları yerine
 * yalnızca {@link org.locationtech.jts.geom.Point} kullanılmalıdır.
 */
@Configuration
public class Wgs84GeometryConfiguration {

	/** Well-Known Text / EPSG: WGS 84 — GPS standardı. */
	public static final int WGS_84_SRID = 4326;

	@Bean
	public GeometryFactory geometryFactory() {
		return new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
	}
}
