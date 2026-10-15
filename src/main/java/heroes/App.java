package heroes;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@SpringBootApplication
public class App {
  public static void main(String[] args) { SpringApplication.run(App.class, args); }
}

record Hero(long id, String name, String alias, String power, String mentor) {}
record NewHero(String name, String alias, String power, String mentor) {}

@RestController
@RequestMapping("/heroes")
class HeroController {
  private static final String COLS = "id, name, alias, power, mentor";
  private static final RowMapper<Hero> MAP = (rs, i) ->
      new Hero(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
  private final JdbcClient jdbc;  // JdbcTemplate underneath (Spring's fluent JDBC API)
  HeroController(JdbcClient jdbc) { this.jdbc = jdbc; }

  @GetMapping
  List<Hero> list() { return jdbc.sql("select " + COLS + " from heroes order by id limit 50").query(MAP).list(); }

  @GetMapping("/{id}")
  ResponseEntity<Hero> get(@PathVariable long id) {
    return ResponseEntity.of(jdbc.sql("select " + COLS + " from heroes where id = ?").param(id).query(MAP).optional());
  }

  @PostMapping
  ResponseEntity<Hero> create(@RequestBody NewHero h) {
    Hero hero = jdbc.sql("insert into heroes (name, alias, power, mentor) values (?, ?, ?, ?) returning " + COLS)
        .params(h.name(), h.alias(), h.power(), h.mentor()).query(MAP).single();
    return ResponseEntity.created(URI.create("/heroes/" + hero.id())).body(hero);
  }

  @PutMapping("/{id}")
  ResponseEntity<Hero> replace(@PathVariable long id, @RequestBody NewHero h) {
    return ResponseEntity.of(jdbc.sql("update heroes set name = ?, alias = ?, power = ?, mentor = ? where id = ? returning " + COLS)
        .params(h.name(), h.alias(), h.power(), h.mentor(), id).query(MAP).optional());
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> remove(@PathVariable long id) {
    int n = jdbc.sql("delete from heroes where id = ?").param(id).update();
    return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
  }
}

/** One JSON line per request via SLF4J structured logging (logging.structured.format.console). */
@Component
class RequestLog extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger("request");
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    long t0 = System.nanoTime();
    try { chain.doFilter(req, res); } finally {
      Object route = req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      log.atInfo().addKeyValue("method", req.getMethod()).addKeyValue("route", route == null ? "" : route)
          .addKeyValue("path", req.getRequestURI()).addKeyValue("status", res.getStatus())
          .addKeyValue("duration_ms", (System.nanoTime() - t0) / 1e6).addKeyValue("client", req.getRemoteAddr())
          .log("request");
    }
  }
}
