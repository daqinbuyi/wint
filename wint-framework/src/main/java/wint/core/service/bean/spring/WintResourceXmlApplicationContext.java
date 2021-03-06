package wint.core.service.bean.spring;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;

import wint.core.config.Constants;
import wint.core.io.resource.loader.ResourceLoader;
import wint.core.service.ServiceContext;
import wint.core.service.aop.ProxyInterceptors;
import wint.core.service.env.Environment;
import wint.core.service.initial.ServiceContextAwire;
import wint.help.sql.dao.AutoLoadSqlMapClientFactoryBean;
import wint.lang.magic.MagicMap;
import wint.lang.magic.MagicObject;
import wint.lang.utils.CollectionUtil;

/**
 * @author pister 2012-3-4 02:51:16
 */
public class WintResourceXmlApplicationContext extends AbstractXmlApplicationContext {

	private String resourceName;

	private ResourceLoader resourceLoader;
	
	private Set<String> profilerSupportBeans;
	
	public WintResourceXmlApplicationContext(String resourceName, final ServiceContext serviceContext) {
		super();
		this.resourceName = resourceName;
		this.resourceLoader = serviceContext.getResourceLoader();
		
		this.profilerSupportBeans = CollectionUtil.newHashSet(Arrays.asList("AO", "Manager", "DAO", "BO", "Service"));
		
		final MagicMap properties = serviceContext.getConfiguration().getProperties();
		final Environment environment = serviceContext.getConfiguration().getEnvironment();
		final boolean sqlmapAutoload = properties.getBoolean(Constants.PropertyKeys.SQLMAP_AUTO_LOAD, Constants.Defaults.SQLMAP_AUTO_LOAD);
		
		this.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				beanFactory.addBeanPostProcessor(new BeanPostProcessor() {

					private boolean needSupportProfiler(Object bean, String beanName) {
						for (String suffix : profilerSupportBeans) {
							if (beanName.endsWith(suffix)) {
								return true;
							}
						}
						return false;
					}
					
					private boolean autoloadSqlmap(Object bean, String beanName) {
						if (environment != Environment.DEV) {
							return false;
						}
						if (!sqlmapAutoload) {
							return false;
						}
						if (bean instanceof SqlMapClientFactoryBean) { 
							return true;
						}
						return false;
					}
					
					public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
						if (needSupportProfiler(bean, beanName)) {
							MagicObject magicObject = MagicObject.wrap(bean);
							return magicObject.asProxyObject(ProxyInterceptors.getInterceptors()).getObject();
						}
						return bean;
					}

					public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
						if (autoloadSqlmap(bean, beanName)) {
							try {
								if (bean instanceof SqlMapClientFactoryBean) {
									AutoLoadSqlMapClientFactoryBean autoLoadConfigSqlMapClientFactoryBean = new AutoLoadSqlMapClientFactoryBean((SqlMapClientFactoryBean)bean);
									autoLoadConfigSqlMapClientFactoryBean.setApplicationContext(WintResourceXmlApplicationContext.this);
									autoLoadConfigSqlMapClientFactoryBean.afterPropertiesSet();
									return autoLoadConfigSqlMapClientFactoryBean;
								}
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
							
						}
						if (bean instanceof ServiceContextAwire) {
							((ServiceContextAwire) bean).setServiceContext(serviceContext);
						}
						return bean;
					}
					
				});
			}
			
		});
		refresh();
	}

	@Override
	protected Resource[] getConfigResources() {
		wint.core.io.resource.Resource wintResource = resourceLoader.getResource(resourceName);
		WintSpringResource wintSpringResource = new WintSpringResource(wintResource);
		return new Resource[] { wintSpringResource };
	}


}
