package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV1 implements ItemRepository {

    private final JdbcTemplate template;

    /**
     * dataSource 를 의존 관계 주입 받고 생성자 내부에서 jdbcTemplate 생성
     -> 스프링에서 jdbcTemplate 사용시 관례상 해당 방법을 많이 사용
     -> 물론 jdbcTemplate 을 스프링 빈으로 직접 등록하고 주입받아도 됨
     */
    public JdbcTemplateItemRepositoryV1(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    /**
     * template.update() -> 데이터 변경시 사용(INSERT, UPDATE, DELETE SQL 에 사용), 영향 받은 로우 수를 반환(int)
     * PK 생성(identity 방식 사용 -> auto increment)
     -> PK 인 ID 값을 개발자가 직접 지정하는 것이 아니라 비워두고 저장, 데이터베이스가 PK 인 ID 를 대신 생성
     -> 데이터베이스에 INSERT 가 완료 되어야 ID 값을 확인할 수 있음
     -> keyHolder 와 prepareStatement 를 사용하여 id 지정시 INSERT 쿼리 실행 이후 데이터베이스에서 생성된 ID 값을 조회할 수 있음
     */
    public Item save(Item item) {
        String sql = "insert into item (item_name, price, quantity) values (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            //자동 증가 키
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, item.getItemName());
            ps.setInt(2, item.getPrice());
            ps.setInt(3, item.getQuantity());
            return ps;
        }, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }

    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name = ?, price  = ?, quantity = ? where id = ?";
        template.update(sql, updateParam.getItemName(), updateParam.getPrice(), updateParam.getQuantity(), itemId);
    }

    /**
     * queryForObject()
     - 결과 row 가 하나일 경우에 사용
     - RowMapper 는 데이터베이스 반환 결과인 ResultSet 을 객체로 변환
     - 결과가 없으면 EmptyResultDataAccessException 예외 발생 -> Optional.of 로 처리 가능
     */
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = ?";

        try {
            Item item = template.queryForObject(sql, itemRowMapper(), id);
            return Optional.of(item);
        } catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        String sql =  "select id, item_name, price, quantity from item";
        //동적쿼리
        if(StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;
        List<Object> param = new ArrayList<>();
        if(StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%', ?, '%')";
            param.add(itemName);
            andFlag = true;
        }

        if(maxPrice != null) {
            if(andFlag) {
                sql += " and";
            }
            sql += " price <= ?";
            param.add(maxPrice);
        }

        log.info("sql = {}", sql);
        return template.query(sql, itemRowMapper(), param.toArray());
    }

    private RowMapper<Item> itemRowMapper() {
        return ((rs, rowNum) -> {
           Item item = new Item();
           item.setId(rs.getLong("id"));
           item.setItemName(rs.getString("item_name"));
           item.setPrice(rs.getInt("price"));
           item.setQuantity(rs.getInt("quantity"));
           return item;
        });
    }
}
