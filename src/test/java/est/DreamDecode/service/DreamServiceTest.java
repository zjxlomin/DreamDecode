package est.DreamDecode.service;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.exception.DreamNotFoundException;
import est.DreamDecode.repository.DreamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    DreamRepository dreamRepository;

    @Mock
    NaturalLanguageService nlpService;

    @Mock
    AnalysisService analysisService;

    @InjectMocks
    DreamService dreamService;

    private Dream dream;

    @BeforeEach
    void setUp() {
        dream = defaultDream();
        dream.setId(1L);
        dream.setCreatedAt(LocalDateTime.now().minusDays(1));
        dream.setUpdatedAt(LocalDateTime.now());
    }

    private Dream defaultDream() {
        return Dream.builder()
                .userId(10L)
                .title("불안한 비행")
                .content("비행기에서 떨어지는 꿈을 꾸었다.")
                .categories("불안,도전")
                .tags("비행기,낙하")
                .published(true)
                .build();
    }

    @Nested
    @DisplayName("공개 꿈 조회")
    class PublicDreamLookup {

        @Test
        @DisplayName("전체 조회 시 DreamResponse 리스트로 변환")
        void getAllPublicDreams_returnsDreamResponses() {
            when(dreamRepository.findAllByPublishedTrue()).thenReturn(List.of(dream));

            List<DreamResponse> responses = dreamService.getAllPublicDreams();

            assertThat(responses).hasSize(1);
            DreamResponse response = responses.get(0);
            assertThat(response.getId()).isEqualTo(dream.getId());
            assertThat(response.getCategories()).containsExactly("불안", "도전");
            assertThat(response.getTags()).containsExactly("비행기", "낙하");
            verify(dreamRepository).findAllByPublishedTrue();
        }

        @Test
        @DisplayName("페이지 조회 시 기본 페이지 사이즈와 정렬 조건을 준수한다")
        void getAllPublicDreams_withPaging() {
            Page<Dream> page = new PageImpl<>(List.of(dream));
            when(dreamRepository.findByPublishedTrueOrderByCreatedAtDesc(any(Pageable.class)))
                    .thenReturn(page);

            Page<DreamResponse> result = dreamService.getAllPublicDreams(2);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo(dream.getTitle());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(dreamRepository).findByPublishedTrueOrderByCreatedAtDesc(pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("카테고리/태그/제목 검색")
    class SearchDreams {

        @Test
        @DisplayName("카테고리 전체 조회 시 repository 결과를 그대로 매핑한다")
        void getDreamsByCategory_withoutPaging() {
            when(dreamRepository.findByCategoriesContaining("불안")).thenReturn(List.of(dream));

            List<DreamResponse> result = dreamService.getDreamsByCategory("불안");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategories()).containsExactly("불안", "도전");
            verify(dreamRepository).findByCategoriesContaining("불안");
        }

        @Test
        @DisplayName("카테고리 페이지 조회 시 페이지 정보가 유지된다")
        void getDreamsByCategory_withPaging() {
            Page<Dream> page = new PageImpl<>(List.of(dream));
            when(dreamRepository.findByCategoriesContaining(eq("불안"), any(Pageable.class)))
                    .thenReturn(page);

            Page<DreamResponse> result = dreamService.getDreamsByCategory("불안", 0);

            assertThat(result.getTotalElements()).isEqualTo(1);
            DreamResponse response = result.stream().findFirst().orElseThrow();
            assertThat(response.getTitle()).isEqualTo(dream.getTitle());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(dreamRepository).findByCategoriesContaining(eq("불안"), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(9);
        }

        @Test
        @DisplayName("태그 전체 조회도 DreamResponse로 변환한다")
        void getDreamsByTag_withoutPaging() {
            when(dreamRepository.findByTagsContaining("낙하")).thenReturn(List.of(dream));

            List<DreamResponse> result = dreamService.getDreamsByTag("낙하");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTags()).containsExactly("비행기", "낙하");
            verify(dreamRepository).findByTagsContaining("낙하");
        }

        @Test
        @DisplayName("태그 페이지 조회 시 페이지 정보가 유지된다")
        void getDreamsByTag_withPaging() {
            Page<Dream> page = new PageImpl<>(List.of(dream));
            when(dreamRepository.findByTagsContaining(eq("낙하"), any(Pageable.class)))
                    .thenReturn(page);

            Page<DreamResponse> result = dreamService.getDreamsByTag("낙하", 1);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(dreamRepository).findByTagsContaining(eq("낙하"), any(Pageable.class));
        }

        @Test
        @DisplayName("제목 전체 조회 시 대소문자 무시 결과를 변환한다")
        void getDreamsByTitle_withoutPaging() {
            when(dreamRepository.findByTitleContaining("비행")).thenReturn(List.of(dream));

            List<DreamResponse> result = dreamService.getDreamsByTitle("비행");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("불안한 비행");
            verify(dreamRepository).findByTitleContaining("비행");
        }

        @Test
        @DisplayName("제목 페이지 조회 시 페이지 정보가 유지된다")
        void getDreamsByTitle_withPaging() {
            Page<Dream> page = new PageImpl<>(List.of(dream));
            when(dreamRepository.findByTitleContaining(eq("비행"), any(Pageable.class)))
                    .thenReturn(page);

            Page<DreamResponse> result = dreamService.getDreamsByTitle("비행", 2);

            assertThat(result.getTotalElements()).isEqualTo(1);
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(dreamRepository).findByTitleContaining(eq("비행"), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("개별 꿈 조회")
    class SingleDreamLookup {

        @Test
        @DisplayName("ID로 꿈 조회 시 존재하면 DreamResponse 반환")
        void getDreamById_found() {
            when(dreamRepository.findById(dream.getId())).thenReturn(Optional.of(dream));

            DreamResponse response = dreamService.getDreamById(dream.getId());

            assertThat(response.getId()).isEqualTo(dream.getId());
            assertThat(response.getContent()).isEqualTo(dream.getContent());
        }

        @Test
        @DisplayName("존재하지 않으면 DreamNotFoundException 발생")
        void getDreamById_notFound() {
            when(dreamRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dreamService.getDreamById(99L))
                    .isInstanceOf(DreamNotFoundException.class)
                    .hasMessageContaining("Dream not found");
        }
    }

    @Test
    @DisplayName("saveDream 호출 시 저장 후 분석 서비스가 실행된다")
    void saveDream_savesAndTriggersAnalysis() {
        DreamRequest request = new DreamRequest();
        request.setUserId(11L);
        request.setTitle("행복한 숲");
        request.setContent("숲에서 노는 꿈");
        request.setPublished(true);

        Dream savedDream = Dream.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .content(request.getContent())
                .categories(null)
                .tags(null)
                .published(request.isPublished())
                .build();
        savedDream.setId(5L);

        when(dreamRepository.save(any(Dream.class))).thenReturn(savedDream);

        Dream result = dreamService.saveDream(request);

        assertThat(result).isEqualTo(savedDream);
        ArgumentCaptor<Dream> dreamCaptor = ArgumentCaptor.forClass(Dream.class);
        verify(dreamRepository).save(dreamCaptor.capture());
        Dream toSave = dreamCaptor.getValue();
        assertThat(toSave.getUserId()).isEqualTo(request.getUserId());
        assertThat(toSave.getTitle()).isEqualTo(request.getTitle());
        assertThat(toSave.getContent()).isEqualTo(request.getContent());

        verify(analysisService).addOrUpdateAnalysis(savedDream.getId(), true);
    }

    @Nested
    @DisplayName("updateDream() 동작")
    class UpdateDreamTests {

        @Test
        @DisplayName("내용이 변경되면 엔티티 업데이트 및 재분석 수행")
        void updateDream_contentChanged() {
            when(dreamRepository.findById(dream.getId())).thenReturn(Optional.of(dream));

            DreamRequest request = new DreamRequest();
            request.setTitle("새로운 꿈");
            request.setContent("새로운 내용");
            request.setUserId(20L);
            request.setPublished(false);

            DreamResponse response = dreamService.updateDream(dream.getId(), request);

            assertThat(dream.getTitle()).isEqualTo("새로운 꿈");
            assertThat(dream.getContent()).isEqualTo("새로운 내용");
            assertThat(dream.getUserId()).isEqualTo(20L);
            assertThat(dream.isPublished()).isFalse();

            assertThat(response.getTitle()).isEqualTo("새로운 꿈");
            verify(analysisService).addOrUpdateAnalysis(dream.getId(), false);
        }

        @Test
        @DisplayName("내용이 변경되지 않으면 재분석을 수행하지 않는다")
        void updateDream_contentNotChanged() {
            when(dreamRepository.findById(dream.getId())).thenReturn(Optional.of(dream));

            DreamRequest request = new DreamRequest();
            request.setTitle("제목만 변경");
            request.setContent(dream.getContent());
            request.setUserId(dream.getUserId());
            request.setPublished(dream.isPublished());

            dreamService.updateDream(dream.getId(), request);

            assertThat(dream.getTitle()).isEqualTo("제목만 변경");
            verify(analysisService, never()).addOrUpdateAnalysis(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("null 필드는 기존 값을 유지한다")
        void updateDream_keepExistingValuesWhenNull() {
            when(dreamRepository.findById(dream.getId())).thenReturn(Optional.of(dream));

            DreamRequest request = new DreamRequest();
            request.setTitle(null);
            request.setContent(null);
            request.setUserId(null);
            request.setPublished(true);

            DreamResponse response = dreamService.updateDream(dream.getId(), request);

            assertThat(response.getTitle()).isEqualTo(dream.getTitle());
            assertThat(response.getContent()).isEqualTo(dream.getContent());
            assertThat(response.getUserId()).isEqualTo(dream.getUserId());
            verify(analysisService, never()).addOrUpdateAnalysis(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("존재하지 않는 꿈 수정 시 DreamNotFoundException 발생")
        void updateDream_notFound() {
            when(dreamRepository.findById(dream.getId())).thenReturn(Optional.empty());

            DreamRequest request = new DreamRequest();
            request.setContent("irrelevant");

            assertThatThrownBy(() -> dreamService.updateDream(dream.getId(), request))
                    .isInstanceOf(DreamNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 꿈 조회")
    class MyDreamLookup {

        @Test
        @DisplayName("전체 목록 조회 시 DreamResponse 리스트로 변환")
        void getMyAllDreams_returnsResponses() {
            when(dreamRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(dream));

            List<DreamResponse> result = dreamService.getMyAllDreams(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(dreamRepository).findByUserIdOrderByCreatedAtDesc(10L);
        }

        @Test
        @DisplayName("limit 기반 조회 시 최소 1 이상으로 페이지 요청한다")
        void getMyDreams_returnsLimitedList() {
            Page<Dream> page = new PageImpl<>(List.of(dream), PageRequest.of(0, 3), 1);
            when(dreamRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);

            List<DreamResponse> responses = dreamService.getMyDreams(10L, 3);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(dream.getId());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(dreamRepository).findByUserIdOrderByCreatedAtDesc(eq(10L), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("limit이 0 이하라도 최소 1개로 보정된다")
        void getMyDreams_limitLessThanOne_defaultsToOne() {
            Page<Dream> page = new PageImpl<>(List.of(dream), PageRequest.of(0, 1), 1);
            when(dreamRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);

            dreamService.getMyDreams(10L, 0);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(dreamRepository).findByUserIdOrderByCreatedAtDesc(eq(10L), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("deleteDream 호출 시 repository가 해당 ID로 삭제한다")
    void deleteDream_deletesById() {
        dreamService.deleteDream(7L);

        verify(dreamRepository).deleteById(7L);
    }
}

