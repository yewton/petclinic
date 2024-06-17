plugins {
    java
     id("io.spring.dependency-management")
}

// 以下で紹介されているやり方を参考に依存関係を管理する。
// Spring Boot plugin が依存しているバージョンの dependency-management プラグインを
// 暗黙的に利用出来ればよいのだがうまく行かないので、
// 明示的にバージョンを指定するようにして対応している( libs.versions.toml 参照 )。
// https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html#managing-dependencies.dependency-management-plugin.using-in-isolation
dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
